import { Component, HostListener, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, Observable } from 'rxjs';
import { Api, Branch, Page, TreasuryAccount, TreasuryEntry, TreasuryInput, TreasuryMovement, errorMessage } from './api';
type Pending={kind:'account';input:TreasuryInput}|{kind:'movement';input:TreasuryMovement};
const empty=<T>():Page<T>=>({items:[],total:0,page:0,size:20});
@Component({standalone:true,imports:[ReactiveFormsModule,DatePipe],templateUrl:'./treasury.html'})
export class Treasury {
 readonly api=inject(Api);private fb=inject(FormBuilder);private sequence=0;
 readonly accounts=signal<Page<TreasuryAccount>>(empty());readonly branches=signal<Page<Branch>>(empty());readonly detail=signal<TreasuryAccount|null>(null);readonly entries=signal<Page<TreasuryEntry>>(empty());
 readonly source=signal<TreasuryAccount|null>(null);readonly destination=signal<TreasuryAccount|null>(null);readonly pending=signal<Pending|null>(null);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');
 readonly selectedBranch=signal<Branch|null>(null);
 readonly form=this.fb.nonNullable.group({branchId:['',Validators.required],code:['',[Validators.required,Validators.pattern('[A-Za-z0-9._-]{1,40}')]],name:['',[Validators.required,Validators.maxLength(160)]],kind:['BANK']});
 readonly movement=this.fb.nonNullable.group({type:['TRANSFER'],amount:['',[Validators.required,Validators.pattern('[0-9]{1,17}(\\.[0-9]{1,2})?')]],note:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]]});
 constructor(){this.load(0);if(this.api.can('branch:read'))this.loadBranches(0);}
 blocked(){return this.busy() || !!this.pending();}
 @HostListener('window:beforeunload',['$event']) unload(event:BeforeUnloadEvent){if(this.pending()){event.preventDefault();event.returnValue='';}}
 canLeave(){if(!this.pending())return true;this.error.set('Confirme o comando pendente antes de sair.');return false;}
 load(page:number){if(this.blocked())return;this.api.treasuryAccounts(page).subscribe({next:v=>this.accounts.set(v),error:e=>this.error.set(errorMessage(e))});}
 loadBranches(page:number){if(this.blocked())return;this.api.branches(page).subscribe({next:v=>this.branches.set(v),error:e=>this.error.set(errorMessage(e))});}
 selectBranch(){this.selectedBranch.set(this.branches().items.find(b=>b.id===this.form.controls.branchId.value)??null);}
 branchVisible(id:string){return this.branches().items.some(b=>b.id===id);}
 open(id:string){if(this.blocked())return;const sequence=++this.sequence;this.detail.set(null);this.entries.set(empty());forkJoin({account:this.api.treasuryAccount(id),entries:this.api.treasuryEntries(id,0)}).subscribe({next:v=>{if(sequence===this.sequence){this.detail.set(v.account);this.entries.set(v.entries);}},error:e=>{if(sequence===this.sequence)this.error.set(errorMessage(e));}});}
 loadEntries(page:number){const a=this.detail(),sequence=this.sequence;if(this.blocked() || !a)return;this.api.treasuryEntries(a.id,page).subscribe({next:v=>{if(sequence===this.sequence)this.entries.set(v);},error:e=>this.error.set(errorMessage(e))});}
 choose(a:TreasuryAccount,side:'source'|'destination'){if(!this.blocked())(side==='source'?this.source:this.destination).set(a);}
 create(){if(this.blocked() || this.form.invalid)return;this.pending.set({kind:'account',input:{...this.form.getRawValue(),requestId:crypto.randomUUID()}});this.send();}
 move(){if(this.blocked() || this.movement.invalid)return;const v=this.movement.getRawValue(),source=v.type==='CREDIT'?null:this.source(),destination=v.type==='DEBIT'?null:this.destination();if((v.type!=='CREDIT'&&!source)||(v.type!=='DEBIT'&&!destination)||source?.id===destination?.id){this.error.set('Selecione as contas necessárias; a transferência exige contas distintas.');return;}this.pending.set({kind:'movement',input:{requestId:crypto.randomUUID(),sourceId:source?.id??null,destinationId:destination?.id??null,amount:v.amount,note:v.note}});this.send();}
 private clear(){this.pending.set(null);this.api.inventoryPending.set(false);this.form.enable();this.movement.enable();}
 send(){const p=this.pending();if(!p || this.busy())return;this.busy.set(true);this.error.set('');this.message.set('');this.form.disable();this.movement.disable();this.api.inventoryPending.set(true);const request:Observable<unknown>=p.kind==='account'?this.api.createTreasuryAccount(p.input):this.api.treasuryMove(p.input);request.subscribe({next:v=>{this.busy.set(false);this.clear();this.message.set(p.kind==='account'?'Conta cadastrada com saldo zero.':'Lançamento interno confirmado.');if(p.kind==='account'){this.form.reset({kind:'BANK'});this.open((v as TreasuryAccount).id);}else{this.movement.reset({type:'TRANSFER'});this.source.set(null);this.destination.set(null);if(this.detail())this.open(this.detail()!.id);}this.load(0);},error:(e:HttpErrorResponse)=>{this.busy.set(false);this.error.set(errorMessage(e));if(e.status!==0&&e.status<500)this.clear();else this.message.set('Resultado incerto. Repita o mesmo comando para confirmar sem duplicar.');}});}
}
