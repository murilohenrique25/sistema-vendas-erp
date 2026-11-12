import { Component, HostListener, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, finalize, Observable } from 'rxjs';
import { Api, Branch, Page, PayableFilter, PayableTotals, Payable, PayableInput, PayablePayment, PayablePaymentInput, Supplier, TreasuryAccount, errorMessage } from './api';

type Pending={kind:'create';input:PayableInput}|{kind:'pay';id:string;input:PayablePaymentInput}|{kind:'reverse';id:string;paymentId:string;input:{requestId:string;note:string}}|{kind:'cancel';id:string;input:{version:number;note:string}};
const empty=<T>():Page<T>=>({items:[],total:0,page:0,size:20});
@Component({standalone:true,imports:[ReactiveFormsModule],templateUrl:'./payables.html'})
export class Payables {
  readonly api=inject(Api);private fb=inject(FormBuilder);private route=inject(ActivatedRoute);private sequence=0;
  readonly receiptFilter=signal('');readonly totals=signal<PayableTotals|null>(null);readonly listLoading=signal(false);private listSequence=0;readonly appliedFilter=signal<{status:string;overdue:boolean;extra:PayableFilter}>({status:'',overdue:false,extra:{}});
  readonly titles=signal<Page<Payable>>(empty());readonly detail=signal<Payable|null>(null);readonly payments=signal<Page<PayablePayment>>(empty());
  readonly branches=signal<Page<Branch>>(empty());readonly suppliers=signal<Page<Supplier>>(empty());readonly selectedSupplier=signal<Supplier|null>(null);readonly selectedBranch=signal<Branch|null>(null);
  readonly treasuryAccounts=signal<Page<TreasuryAccount>>(empty());readonly selectedAccount=signal<TreasuryAccount|null>(null);
  readonly calendar=signal<{date:string;zone:string}|null>(null);readonly busy=signal(false);readonly loading=signal(false);readonly error=signal('');readonly message=signal('');readonly pending=signal<Pending|null>(null);readonly reversing=signal<PayablePayment|null>(null);
  readonly form=this.fb.nonNullable.group({branchId:['',Validators.required],reference:['',[Validators.required,Validators.pattern('[A-Za-z0-9._-]{1,60}')]],document:['',Validators.maxLength(160)],description:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]],issuedOn:['',Validators.required],dueOn:['',Validators.required],amount:['',[Validators.required,Validators.pattern('[0-9]{1,17}(\\.[0-9]{1,2})?')]],purchaseReceiptId:['',Validators.pattern('([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})?')]});
  readonly filter=this.fb.nonNullable.group({status:[''],overdue:[false],query:['',Validators.maxLength(200)],dueFrom:[''],dueTo:[''],withReceipt:['']});readonly supplierSearch=this.fb.nonNullable.group({q:['',Validators.maxLength(200)]});
  readonly paymentForm=this.fb.nonNullable.group({amount:['',[Validators.required,Validators.pattern('[0-9]{1,17}(\\.[0-9]{1,2})?')]],paidOn:['',Validators.required],method:['BANK_TRANSFER'],treasuryAccountId:[''],reference:['',Validators.maxLength(160)],note:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]]});
  readonly reversalForm=this.fb.nonNullable.group({note:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]]});readonly cancellationForm=this.fb.nonNullable.group({note:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]]});
  constructor(){
    const sourceReceipt=this.route.snapshot.queryParamMap.get('sourceReceipt');if(sourceReceipt&&/^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$/.test(sourceReceipt)){this.receiptFilter.set(sourceReceipt);this.appliedFilter.set({status:'',overdue:false,extra:{receiptId:sourceReceipt}});}
    this.load(0);this.loadTreasury(0);if(this.api.can('branch:read'))this.loadBranches(0);if(this.api.can('supplier:read'))this.loadSuppliers(0);
    this.api.payableCalendar().subscribe({next:value=>{this.calendar.set(value);if(!this.form.controls.issuedOn.value)this.form.controls.issuedOn.setValue(value.date);if(!this.paymentForm.controls.paidOn.value)this.paymentForm.controls.paidOn.setValue(value.date);},error:e=>this.fail(e)});
    const params=this.route.snapshot.queryParamMap,uuid=/^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$/;
    const receipt=params.get('receiptId'),branch=params.get('branchId'),supplier=params.get('supplierId');
    const title=params.get('title');if(title&&uuid.test(title))this.open(title);
    if(receipt && uuid.test(receipt))this.form.controls.purchaseReceiptId.setValue(receipt);
    if(branch && uuid.test(branch)){this.form.controls.branchId.setValue(branch);if(this.api.can('branch:read'))this.api.branch(branch).subscribe({next:value=>this.selectedBranch.set(value),error:e=>this.fail(e)});}
    if(supplier && uuid.test(supplier) && this.api.can('supplier:read'))this.api.supplier(supplier).subscribe({next:value=>this.selectedSupplier.set(value),error:e=>this.fail(e)});
  }
  @HostListener('window:beforeunload',['$event']) unload(event:BeforeUnloadEvent){if(this.pending()){event.preventDefault();event.returnValue='';}}
  canLeave(){if(!this.pending())return true;this.error.set('Repita ou conclua o comando pendente antes de sair.');return false;}
  blocked(){return this.busy() || !!this.pending();}
  date(value:string){return value.split('-').reverse().join('/');}
  state(value:string){return ({OPEN:'Em aberto',PARTIAL:'Pago parcialmente',PAID:'Pago',CREDITED:'Quitado com crédito',CANCELLED:'Cancelado'} as Record<string,string>)[value]??value;}
  method(value:string){return ({CASH:'Dinheiro',BANK_TRANSFER:'Transferência bancária',PIX:'Pix',OTHER:'Outro'} as Record<string,string>)[value]??value;}
  canLinkTreasury(){return this.api.can('treasury:read') && this.api.can('treasury:move');}
  loadTreasury(page:number){if(this.blocked() || !this.canLinkTreasury())return;this.api.treasuryAccounts(page).subscribe({next:v=>this.treasuryAccounts.set(v),error:e=>this.fail(e)});}
  pickAccount(account:TreasuryAccount|null){if(this.blocked())return;this.selectedAccount.set(account);this.paymentForm.controls.treasuryAccountId.setValue(account?.id??'');}
  private fail(error:HttpErrorResponse){this.error.set(errorMessage(error));}
  branchVisible(id:string){return this.branches().items.some(branch=>branch.id===id);}
  applyFilter(){if(this.blocked()||this.filter.invalid)return;const value=this.filter.getRawValue();if(value.dueFrom&&value.dueTo&&value.dueFrom>value.dueTo){this.error.set('Vencimento inicial deve ser anterior ou igual ao final.');return;}const extra:PayableFilter={};if(value.query.trim())extra.query=value.query.trim();if(value.dueFrom)extra.dueFrom=value.dueFrom;if(value.dueTo)extra.dueTo=value.dueTo;if(value.withReceipt)extra.withReceipt=value.withReceipt==='true';if(this.receiptFilter())extra.receiptId=this.receiptFilter();this.appliedFilter.set({status:value.status,overdue:value.overdue,extra});this.titles.set(empty());this.totals.set(null);this.error.set('');this.load(0);}
  clearFilter(){if(this.blocked())return;this.filter.reset();this.receiptFilter.set('');this.applyFilter();}
  relatedReceipt(){const title=this.detail();if(this.blocked()||!title?.purchaseReceiptId)return;this.filter.reset();this.receiptFilter.set(title.purchaseReceiptId);this.applyFilter();}
  clearReceiptFilter(){if(this.blocked())return;this.receiptFilter.set('');this.applyFilter();}
  load(page:number){if(this.blocked())return;const sequence=++this.listSequence;const filter=this.appliedFilter();this.listLoading.set(true);this.api.payables(filter.status,filter.overdue,page,filter.extra).subscribe({next:value=>{if(sequence===this.listSequence){this.listLoading.set(false);this.titles.set(value);this.totals.set(value.summary);}},error:e=>{if(sequence===this.listSequence){this.listLoading.set(false);this.fail(e);}}});}

  loadBranches(page:number){if(this.blocked())return;this.api.branches(page).subscribe({next:value=>this.branches.set(value),error:e=>this.fail(e)});}
  loadSuppliers(page:number){if(this.blocked() || this.supplierSearch.invalid)return;this.api.suppliers(this.supplierSearch.controls.q.value,page).subscribe({next:value=>this.suppliers.set(value),error:e=>this.fail(e)});}
  pickSupplier(value:Supplier){if(!this.blocked())this.selectedSupplier.set(value);}
  create(){
    const supplier=this.selectedSupplier();if(this.blocked() || this.form.invalid || !supplier)return;
    const form=this.form.getRawValue();this.pending.set({kind:'create',input:{...form,purchaseReceiptId:form.purchaseReceiptId||null,supplierId:supplier.id,requestId:crypto.randomUUID()}});this.sendPending();
  }
  open(id:string){
    if(this.blocked())return;const sequence=++this.sequence;this.detail.set(null);this.payments.set(empty());this.reversing.set(null);this.selectedAccount.set(null);this.reversalForm.reset();this.cancellationForm.reset();this.paymentForm.reset({method:'BANK_TRANSFER',paidOn:this.calendar()?.date??''});this.loading.set(true);
    forkJoin({title:this.api.payable(id),payments:this.api.payablePayments(id,0)}).pipe(finalize(()=>{if(sequence===this.sequence)this.loading.set(false);})).subscribe({next:value=>{if(sequence===this.sequence){this.detail.set(value.title);this.payments.set(value.payments);}},error:e=>{if(sequence===this.sequence)this.fail(e);}});
  }
  loadPayments(page:number){const title=this.detail();if(this.blocked() || !title)return;const sequence=this.sequence;this.api.payablePayments(title.id,page).subscribe({next:value=>{if(sequence===this.sequence)this.payments.set(value);},error:e=>{if(sequence===this.sequence)this.fail(e);}});}
  pay(){const title=this.detail();if(this.blocked() || !title || this.paymentForm.invalid)return;this.pending.set({kind:'pay',id:title.id,input:{...this.paymentForm.getRawValue(),treasuryAccountId:this.paymentForm.controls.treasuryAccountId.value||null,paymentId:crypto.randomUUID()}});this.sendPending();}
  chooseReversal(payment:PayablePayment){if(!this.blocked()){this.reversing.set(payment);this.reversalForm.reset();}}
  reverse(){const title=this.detail(),payment=this.reversing();if(this.blocked() || !title || !payment || this.reversalForm.invalid)return;this.pending.set({kind:'reverse',id:title.id,paymentId:payment.id,input:{requestId:crypto.randomUUID(),note:this.reversalForm.controls.note.value}});this.sendPending();}
  cancel(){const title=this.detail();if(this.blocked() || !title || this.cancellationForm.invalid)return;this.pending.set({kind:'cancel',id:title.id,input:{version:title.version,note:this.cancellationForm.controls.note.value}});this.sendPending();}
  private clearPending(){this.pending.set(null);this.api.inventoryPending.set(false);this.form.enable();this.paymentForm.enable();this.reversalForm.enable();this.cancellationForm.enable();}
  sendPending(){
    const command=this.pending();if(!command || this.busy())return;this.busy.set(true);this.error.set('');this.message.set('');this.api.inventoryPending.set(true);this.form.disable();this.paymentForm.disable();this.reversalForm.disable();this.cancellationForm.disable();
    const request:Observable<unknown>=command.kind==='create'?this.api.createPayable(command.input):command.kind==='pay'?this.api.payPayable(command.id,command.input):command.kind==='reverse'?this.api.reversePayablePayment(command.id,command.paymentId,command.input):this.api.cancelPayable(command.id,command.input);
    request.subscribe({next:value=>{
      this.busy.set(false);this.clearPending();this.message.set(command.kind==='create'?'Título criado.':command.kind==='pay'?'Registro de pagamento confirmado.':command.kind==='reverse'?'Registro estornado; saldo do título atualizado.':'Título cancelado.');
      this.loadTreasury(0);if(command.kind==='create'){this.form.reset({issuedOn:this.calendar()?.date??''});this.selectedSupplier.set(null);this.selectedBranch.set(null);this.open((value as Payable).id);}else this.open(command.id);this.load(0);
    },error:(error:HttpErrorResponse)=>{this.busy.set(false);this.fail(error);if(error.status!==0 && error.status<500)this.clearPending();else this.message.set('Resultado incerto. Repita o mesmo comando para confirmar sem duplicar.');}});
  }
}
