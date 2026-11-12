import { Component, HostListener, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, Observable } from 'rxjs';
import { Api, Branch, Page, TreasuryAccount, PosTerminal, PosTerminalInput, PosCashOpening, PosCashSession, PosCashMoveInput, PosCashMovement, PosCashClosing, errorMessage } from './api';
type Pending={kind:'terminal';input:PosTerminalInput}|{kind:'active';id:string;input:{active:boolean;version:number}}|{kind:'open';input:PosCashOpening}|{kind:'move';id:string;input:PosCashMoveInput}|{kind:'close';id:string;input:PosCashClosing};
const empty=<T>():Page<T>=>({items:[],total:0,page:0,size:20});
@Component({standalone:true,imports:[ReactiveFormsModule,DatePipe],templateUrl:'./pos-cash.html'})
export class PosCash {
 readonly api=inject(Api);private fb=inject(FormBuilder);private sequence=0;
 readonly terminals=signal<Page<PosTerminal>>(empty());readonly sessions=signal<Page<PosCashSession>>(empty());readonly movements=signal<Page<PosCashMovement>>(empty());readonly branches=signal<Page<Branch>>(empty());readonly accounts=signal<Page<TreasuryAccount>>(empty());
 readonly chosenTerminal=signal<PosTerminal|null>(null);readonly openingAccount=signal<TreasuryAccount|null>(null);readonly selectedBranch=signal<Branch|null>(null);readonly terminalAccount=signal<TreasuryAccount|null>(null);readonly otherAccount=signal<TreasuryAccount|null>(null);readonly detail=signal<PosCashSession|null>(null);readonly operatorId=signal('');
 readonly pending=signal<Pending|null>(null);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');
 readonly terminalForm=this.fb.nonNullable.group({branchId:['',Validators.required],code:['',[Validators.required,Validators.pattern('[A-Za-z0-9._-]{1,40}')]],name:['',[Validators.required,Validators.maxLength(160)]]});
 readonly opening=this.fb.nonNullable.group({openingAmount:['',[Validators.required,Validators.pattern('[0-9]{1,17}(\\.[0-9]{1,2})?')]],note:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]]});
 readonly moveForm=this.fb.nonNullable.group({kind:['SUPPLY'],amount:['',[Validators.required,Validators.pattern('[0-9]{1,17}(\\.[0-9]{1,2})?')]],note:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]]});
 readonly closing=this.fb.nonNullable.group({countedAmount:['',[Validators.required,Validators.pattern('[0-9]{1,17}(\\.[0-9]{1,2})?')]],note:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]]});readonly filter=this.fb.nonNullable.group({status:['']});
 constructor(){this.loadTerminals(0);this.loadSessions(0);this.loadAccounts(0);if(this.api.can('branch:read'))this.loadBranches(0);this.api.posOperator().subscribe({next:v=>this.operatorId.set(v.userId),error:e=>this.fail(e)});}
 blocked(){return this.busy()||!!this.pending();}
 @HostListener('window:beforeunload',['$event']) unload(e:BeforeUnloadEvent){if(this.pending()){e.preventDefault();e.returnValue='';}}
 canLeave(){if(!this.pending())return true;this.error.set('Confirme o comando pendente antes de sair.');return false;}
 canOperateSession(){const s=this.detail();return !!s&&(s.operatorId===this.operatorId()||this.api.can('pos:supervise'));}
 private fail(e:HttpErrorResponse){this.error.set(errorMessage(e));}
 loadTerminals(page:number){if(this.blocked())return;this.api.posTerminals(page).subscribe({next:v=>this.terminals.set(v),error:e=>this.fail(e)});}
 loadSessions(page:number){if(this.blocked())return;this.api.posCashSessions(this.filter.controls.status.value,page).subscribe({next:v=>this.sessions.set(v),error:e=>this.fail(e)});}
 loadAccounts(page:number){if(this.blocked())return;this.api.treasuryAccounts(page).subscribe({next:v=>this.accounts.set(v),error:e=>this.fail(e)});}
 loadBranches(page:number){if(this.blocked())return;this.api.branches(page).subscribe({next:v=>this.branches.set(v),error:e=>this.fail(e)});}
 selectBranch(){this.selectedBranch.set(this.branches().items.find(b=>b.id===this.terminalForm.controls.branchId.value)??null);this.terminalAccount.set(null);}
 branchVisible(id:string){return this.branches().items.some(b=>b.id===id);}
 chooseTerminalAccount(a:TreasuryAccount){if(!this.blocked())this.terminalAccount.set(a);}
 chooseOther(a:TreasuryAccount|null){if(!this.blocked())this.otherAccount.set(a);}
 chooseTerminal(t:PosTerminal){if(this.blocked())return;this.chosenTerminal.set(t);this.openingAccount.set(null);this.opening.reset();this.api.treasuryAccount(t.accountId).subscribe({next:a=>{if(this.chosenTerminal()?.id===t.id)this.openingAccount.set(a);},error:e=>this.fail(e)});}
 createTerminal(){const a=this.terminalAccount();if(this.blocked()||this.terminalForm.invalid||!a)return;this.pending.set({kind:'terminal',input:{...this.terminalForm.getRawValue(),accountId:a.id,requestId:crypto.randomUUID()}});this.send();}
 toggle(t:PosTerminal){if(this.blocked())return;this.pending.set({kind:'active',id:t.id,input:{active:!t.active,version:t.version}});this.send();}
 openCash(){const t=this.chosenTerminal();if(this.blocked()||!t||!this.openingAccount()||this.opening.invalid)return;this.pending.set({kind:'open',input:{...this.opening.getRawValue(),terminalId:t.id,requestId:crypto.randomUUID()}});this.send();}
 inspect(id:string){if(this.blocked())return;const seq=++this.sequence;this.detail.set(null);this.movements.set(empty());this.otherAccount.set(null);this.moveForm.reset({kind:'SUPPLY'});this.closing.reset();forkJoin({session:this.api.posCashSession(id),moves:this.api.posCashMovements(id,0)}).subscribe({next:v=>{if(seq===this.sequence){this.detail.set(v.session);this.movements.set(v.moves);}},error:e=>{if(seq===this.sequence)this.fail(e);}});}
 loadMovements(page:number){const s=this.detail(),seq=this.sequence;if(this.blocked()||!s)return;this.api.posCashMovements(s.id,page).subscribe({next:v=>{if(seq===this.sequence)this.movements.set(v);},error:e=>this.fail(e)});}
 move(){const s=this.detail();if(this.blocked()||!s||this.moveForm.invalid||!this.canOperateSession())return;this.pending.set({kind:'move',id:s.id,input:{...this.moveForm.getRawValue(),otherAccountId:this.otherAccount()?.id??null,requestId:crypto.randomUUID()}});this.send();}
 close(){const s=this.detail();if(this.blocked()||!s||this.closing.invalid||!this.canOperateSession())return;this.pending.set({kind:'close',id:s.id,input:{...this.closing.getRawValue(),version:s.version,requestId:crypto.randomUUID()}});this.send();}
 private clear(){this.pending.set(null);this.api.inventoryPending.set(false);this.terminalForm.enable();this.opening.enable();this.moveForm.enable();this.closing.enable();this.filter.enable();}
 send(){const p=this.pending();if(!p||this.busy())return;this.busy.set(true);this.error.set('');this.message.set('');this.api.inventoryPending.set(true);this.terminalForm.disable();this.opening.disable();this.moveForm.disable();this.closing.disable();this.filter.disable();const request:Observable<unknown>=p.kind==='terminal'?this.api.createPosTerminal(p.input):p.kind==='active'?this.api.setPosTerminalActive(p.id,p.input):p.kind==='open'?this.api.openPosCash(p.input):p.kind==='move'?this.api.movePosCash(p.id,p.input):this.api.closePosCash(p.id,p.input);request.subscribe({next:v=>{this.busy.set(false);this.clear();this.message.set(p.kind==='terminal'?'Terminal cadastrado.':p.kind==='active'?'Situação do terminal atualizada.':p.kind==='open'?'Caixa aberto.':p.kind==='move'?'Movimento confirmado na sessão e na tesouraria.':'Caixa fechado; contagem e divergência registradas.');if(p.kind==='open'){this.chosenTerminal.set(null);this.openingAccount.set(null);this.opening.reset();this.inspect((v as PosCashSession).id);}else if(p.kind==='move'||p.kind==='close')this.inspect(p.id);else if(p.kind==='terminal'){this.terminalForm.reset();this.selectedBranch.set(null);this.terminalAccount.set(null);}this.loadTerminals(0);this.loadSessions(0);this.loadAccounts(0);},error:(e:HttpErrorResponse)=>{this.busy.set(false);this.fail(e);if(e.status!==0&&e.status<500){this.clear();this.loadTerminals(0);this.loadSessions(0);if(p.kind==='move'||p.kind==='close')this.inspect(p.id);}else this.message.set('Resultado incerto. Repita o mesmo comando para confirmar sem duplicar.');}});}
}
