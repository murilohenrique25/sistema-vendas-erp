import { Component, HostListener, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, finalize, Observable } from 'rxjs';
import { Api, Branch, Page, ReceivableFilter, ReceivableTotals, ReceivableOrigin, Receivable, ReceivableInput, ReceivablePlanInput, ReceivablePlanPreview, ReceivablePlan, ReceivableAdjustment, ReceivableReceipt, ReceivableReceiptInput, Customer, TreasuryAccount, errorMessage } from './api';

type Pending={kind:'create';input:ReceivableInput}|{kind:'plan';input:ReceivablePlanInput}|{kind:'receive';id:string;input:ReceivableReceiptInput}|{kind:'reverse';id:string;receiptId:string;input:{requestId:string;note:string}}|{kind:'cancel';id:string;input:{version:number;note:string}};
const empty=<T>():Page<T>=>({items:[],total:0,page:0,size:20});
@Component({standalone:true,imports:[ReactiveFormsModule,DatePipe],templateUrl:'./receivables.html'})
export class Receivables {
  readonly api=inject(Api);private fb=inject(FormBuilder);private sequence=0;
  readonly origin=signal<ReceivableOrigin|null>(null);readonly planFilter=signal('');readonly totals=signal<ReceivableTotals|null>(null);readonly listLoading=signal(false);private listSequence=0;readonly appliedFilter=signal<{status:string;overdue:boolean;extra:ReceivableFilter}>({status:'',overdue:false,extra:{}});
  readonly adjustments=signal<Page<ReceivableAdjustment>>(empty());
  readonly titles=signal<Page<Receivable>>(empty());readonly detail=signal<Receivable|null>(null);readonly receipts=signal<Page<ReceivableReceipt>>(empty());
  readonly branches=signal<Page<Branch>>(empty());readonly customers=signal<Page<Customer>>(empty());readonly selectedCustomer=signal<Customer|null>(null);readonly selectedBranch=signal<Branch|null>(null);
  readonly treasuryAccounts=signal<Page<TreasuryAccount>>(empty());readonly selectedAccount=signal<TreasuryAccount|null>(null);
  readonly calendar=signal<{date:string;zone:string}|null>(null);readonly busy=signal(false);readonly loading=signal(false);readonly error=signal('');readonly message=signal('');readonly pending=signal<Pending|null>(null);readonly reversing=signal<ReceivableReceipt|null>(null);
  readonly terms=this.fb.nonNullable.group({installmentCount:[1,[Validators.required,Validators.min(1),Validators.max(120),Validators.pattern('[0-9]+')]],intervalMonths:[1,[Validators.required,Validators.min(1),Validators.max(12),Validators.pattern('[0-9]+')]]});
  readonly preview=signal<{key:string;value:ReceivablePlanPreview}|null>(null);readonly previewBusy=signal(false);
  readonly form=this.fb.nonNullable.group({branchId:['' ,Validators.required],reference:['',[Validators.required,Validators.pattern('[A-Za-z0-9._-]{1,60}')]],document:['',Validators.maxLength(160)],description:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]],issuedOn:['',Validators.required],dueOn:['',Validators.required],amount:['',[Validators.required,Validators.pattern('[0-9]{1,17}(\\.[0-9]{1,2})?')]]});
  readonly filter=this.fb.nonNullable.group({status:[''],overdue:[false],query:['',Validators.maxLength(200)],dueFrom:[''],dueTo:[''],origin:['']});readonly customerSearch=this.fb.nonNullable.group({q:['',Validators.maxLength(200)]});
  readonly receiptForm=this.fb.nonNullable.group({amount:['',[Validators.required,Validators.pattern('[0-9]{1,17}(\\.[0-9]{1,2})?')]],receivedOn:['',Validators.required],method:['BANK_TRANSFER'],treasuryAccountId:[''],reference:['',Validators.maxLength(160)],note:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]]});
  readonly reversalForm=this.fb.nonNullable.group({note:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]]});readonly cancellationForm=this.fb.nonNullable.group({note:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]]});
  constructor(){
    const params=inject(ActivatedRoute).snapshot.queryParamMap,uuid=/^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$/;
    const sourcePlan=params.get('sourcePlan');if(sourcePlan&&uuid.test(sourcePlan)){this.planFilter.set(sourcePlan);this.appliedFilter.set({status:'',overdue:false,extra:{planId:sourcePlan}});}
    const titleId=params.get('title');if(titleId&&uuid.test(titleId))this.open(titleId);
    this.load(0);this.loadTreasury(0);if(this.api.can('branch:read'))this.loadBranches(0);if(this.api.can('customer:read'))this.loadCustomers(0);
    this.api.receivableCalendar().subscribe({next:value=>{this.calendar.set(value);if(!this.form.controls.issuedOn.value)this.form.controls.issuedOn.setValue(value.date);if(!this.receiptForm.controls.receivedOn.value)this.receiptForm.controls.receivedOn.setValue(value.date);},error:e=>this.fail(e)});
  }
  @HostListener('window:beforeunload',['$event']) unload(event:BeforeUnloadEvent){if(this.pending()){event.preventDefault();event.returnValue='';}}
  canLeave(){if(!this.pending())return true;this.error.set('Repita ou conclua o comando pendente antes de sair.');return false;}
  blocked(){return this.busy() || !!this.pending() || this.previewBusy();}
  private planInput():ReceivablePlanInput|null{const customer=this.selectedCustomer();if(!customer || this.form.invalid || this.terms.invalid)return null;const {dueOn,...form}=this.form.getRawValue();return {...form,...this.terms.getRawValue(),firstDueOn:dueOn,customerId:customer.id,requestId:''};}
  currentPreview(){const input=this.planInput(),preview=this.preview();return input && preview?.key===JSON.stringify(input)?preview.value:null;}
  previewPlan(){const input=this.planInput();if(this.blocked() || !input)return;this.previewBusy.set(true);this.error.set('');this.preview.set(null);this.api.previewReceivablePlan({...input,requestId:crypto.randomUUID()}).pipe(finalize(()=>this.previewBusy.set(false))).subscribe({next:value=>this.preview.set({key:JSON.stringify(input),value}),error:e=>this.fail(e)});}
  date(value:string){return value.split('-').reverse().join('/');}
  state(value:string){return ({OPEN:'Em aberto',PARTIAL:'Recebido parcialmente',RECEIVED:'Recebido',CREDITED:'Liquidado com abatimento',CANCELLED:'Cancelado'} as Record<string,string>)[value]??value;}
  method(value:string){return ({CASH:'Dinheiro',BANK_TRANSFER:'Transferência bancária',PIX:'Pix',OTHER:'Outro'} as Record<string,string>)[value]??value;}
  canLinkTreasury(){return this.api.can('treasury:read') && this.api.can('treasury:move');}
  loadTreasury(page:number){if(this.blocked() || !this.canLinkTreasury())return;this.api.treasuryAccounts(page).subscribe({next:v=>this.treasuryAccounts.set(v),error:e=>this.fail(e)});}
  pickAccount(account:TreasuryAccount|null){if(this.blocked())return;this.selectedAccount.set(account);this.receiptForm.controls.treasuryAccountId.setValue(account?.id??'');}
  private fail(error:HttpErrorResponse){this.error.set(errorMessage(error));}
  selectBranch(){this.selectedBranch.set(this.branches().items.find(b=>b.id===this.form.controls.branchId.value)??null);}
  branchVisible(id:string){return this.branches().items.some(branch=>branch.id===id);}
  applyFilter(){if(this.blocked()||this.filter.invalid)return;const value=this.filter.getRawValue();if(value.dueFrom&&value.dueTo&&value.dueFrom>value.dueTo){this.error.set('Vencimento inicial deve ser anterior ou igual ao final.');return;}const extra:ReceivableFilter={};if(value.query.trim())extra.query=value.query.trim();if(value.dueFrom)extra.dueFrom=value.dueFrom;if(value.dueTo)extra.dueTo=value.dueTo;if(value.origin)extra.origin=value.origin;if(this.planFilter())extra.planId=this.planFilter();this.appliedFilter.set({status:value.status,overdue:value.overdue,extra});this.titles.set(empty());this.totals.set(null);this.error.set('');this.load(0);}
  clearFilter(){if(this.blocked())return;this.filter.reset();this.planFilter.set('');this.applyFilter();}
  relatedPlan(){const origin=this.origin();if(this.blocked()||!origin?.planId)return;this.filter.reset();this.planFilter.set(origin.planId);this.applyFilter();}
  clearPlanFilter(){if(this.blocked())return;this.planFilter.set('');this.applyFilter();}
  load(page:number){if(this.blocked())return;const sequence=++this.listSequence,filter=this.appliedFilter();this.listLoading.set(true);this.api.receivables(filter.status,filter.overdue,page,filter.extra).subscribe({next:value=>{if(sequence===this.listSequence){this.listLoading.set(false);this.titles.set(value);this.totals.set(value.summary);}},error:e=>{if(sequence===this.listSequence){this.listLoading.set(false);this.fail(e);}}});}

  loadBranches(page:number){if(this.blocked())return;this.api.branches(page).subscribe({next:value=>this.branches.set(value),error:e=>this.fail(e)});}
  loadCustomers(page:number){if(this.blocked() || this.customerSearch.invalid)return;this.api.customers(this.customerSearch.controls.q.value,page).subscribe({next:value=>this.customers.set(value),error:e=>this.fail(e)});}
  pickCustomer(value:Customer){if(!this.blocked())this.selectedCustomer.set(value);}
  create(){
    const customer=this.selectedCustomer();if(this.blocked() || this.form.invalid || !customer)return;
    if(this.terms.invalid)return;
    if(this.terms.controls.installmentCount.value>1){const input=this.planInput();if(!input || !this.currentPreview()){this.error.set('Confira a prévia das parcelas antes de criar.');return;}this.pending.set({kind:'plan',input:{...input,requestId:crypto.randomUUID()}});this.sendPending();return;}
    const form=this.form.getRawValue();this.pending.set({kind:'create',input:{...form,customerId:customer.id,requestId:crypto.randomUUID()}});this.sendPending();
  }
  open(id:string){
    if(this.blocked())return;const sequence=++this.sequence;this.origin.set(null);this.detail.set(null);this.receipts.set(empty());this.adjustments.set(empty());this.reversing.set(null);this.selectedAccount.set(null);this.reversalForm.reset();this.cancellationForm.reset();this.receiptForm.reset({method:'BANK_TRANSFER',receivedOn:this.calendar()?.date??''});this.loading.set(true);
    forkJoin({title:this.api.receivable(id),receipts:this.api.receivableReceipts(id,0),adjustments:this.api.receivableAdjustments(id,0),origin:this.api.receivableOrigin(id)}).pipe(finalize(()=>{if(sequence===this.sequence)this.loading.set(false);})).subscribe({next:value=>{if(sequence===this.sequence){this.detail.set(value.title);this.origin.set(value.origin);this.receipts.set(value.receipts);this.adjustments.set(value.adjustments);}},error:e=>{if(sequence===this.sequence)this.fail(e);}});
  }
  loadAdjustments(page:number){const title=this.detail();if(this.blocked()||!title)return;const sequence=this.sequence;this.api.receivableAdjustments(title.id,page).subscribe({next:value=>{if(sequence===this.sequence)this.adjustments.set(value);},error:e=>{if(sequence===this.sequence)this.fail(e);}});}
  loadReceipts(page:number){const title=this.detail();if(this.blocked() || !title)return;const sequence=this.sequence;this.api.receivableReceipts(title.id,page).subscribe({next:value=>{if(sequence===this.sequence)this.receipts.set(value);},error:e=>{if(sequence===this.sequence)this.fail(e);}});}
  receive(){const title=this.detail();if(this.blocked() || !title || this.receiptForm.invalid)return;this.pending.set({kind:'receive',id:title.id,input:{...this.receiptForm.getRawValue(),treasuryAccountId:this.receiptForm.controls.treasuryAccountId.value||null,receiptId:crypto.randomUUID()}});this.sendPending();}
  chooseReversal(receipt:ReceivableReceipt){if(!this.blocked()){this.reversing.set(receipt);this.reversalForm.reset();}}
  reverse(){const title=this.detail(),receipt=this.reversing();if(this.blocked() || !title || !receipt || this.reversalForm.invalid)return;this.pending.set({kind:'reverse',id:title.id,receiptId:receipt.id,input:{requestId:crypto.randomUUID(),note:this.reversalForm.controls.note.value}});this.sendPending();}
  cancel(){const title=this.detail();if(this.blocked() || !title || this.cancellationForm.invalid)return;this.pending.set({kind:'cancel',id:title.id,input:{version:title.version,note:this.cancellationForm.controls.note.value}});this.sendPending();}
  private clearPending(){this.pending.set(null);this.api.inventoryPending.set(false);this.form.enable();this.terms.enable();this.receiptForm.enable();this.reversalForm.enable();this.cancellationForm.enable();}
  sendPending(){
    const command=this.pending();if(!command || this.busy())return;this.busy.set(true);this.error.set('');this.message.set('');this.api.inventoryPending.set(true);this.form.disable();this.receiptForm.disable();this.reversalForm.disable();this.cancellationForm.disable();
    this.terms.disable();
    const request:Observable<unknown>=command.kind==='plan'?this.api.createReceivablePlan(command.input):command.kind==='create'?this.api.createReceivable(command.input):command.kind==='receive'?this.api.receiveReceivable(command.id,command.input):command.kind==='reverse'?this.api.reverseReceivableReceipt(command.id,command.receiptId,command.input):this.api.cancelReceivable(command.id,command.input);
    request.subscribe({next:value=>{
      this.busy.set(false);this.clearPending();this.message.set(command.kind==='plan'?'Parcelas criadas. Cada título pode receber baixas parciais e estornos.':command.kind==='create'?'Título criado.':command.kind==='receive'?'Registro de recebimento confirmado.':command.kind==='reverse'?'Registro estornado; saldo do título atualizado.':'Título cancelado.');
      this.loadTreasury(0);if(command.kind==='create' || command.kind==='plan'){this.form.reset({issuedOn:this.calendar()?.date??''});this.terms.reset({installmentCount:1,intervalMonths:1});this.preview.set(null);this.selectedCustomer.set(null);this.selectedBranch.set(null);this.open(command.kind==='plan'?(value as ReceivablePlan).installments[0].id:(value as Receivable).id);}else this.open(command.id);this.load(0);
    },error:(error:HttpErrorResponse)=>{this.busy.set(false);this.fail(error);if(error.status!==0 && error.status<500)this.clearPending();else this.message.set('Resultado incerto. Repita o mesmo comando para confirmar sem duplicar.');}});
  }
}
