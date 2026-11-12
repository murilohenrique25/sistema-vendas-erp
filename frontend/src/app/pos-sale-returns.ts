import { Component, EventEmitter, HostListener, Input, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Api, Page, PosCashSession, TreasuryAccount, errorMessage } from './api';
import { Sale, Item, Payment } from './pos-sale-models';
interface ReceiptSource {id:string;receivableId:string;titleReference:string;method:string;reference:string;amount:string;refundedAmount:string;refundable:string;treasuryAccountId:string|null;accountKind:string|null}
interface ReceiptChoice {source:ReceiptSource;amount:string;method:string;accountId:string;reference:string}
interface ReturnInput {expectedDebtReduction?:string;receiptRefunds?:{receiptId:string;amount:string;method:string;accountId:string|null;reference:string}[];requestId:string;sessionId:string;version:number;note:string;externalRefundsConfirmed:boolean;items:{productId:string;quantity:string}[];refunds:{paymentId:string;amount:string;reference:string}[]}
interface Return {debtReduction:string;debtCredits:{receivableId:string;reference:string;amount:string}[];receiptRefunds:{receiptId:string;amount:string;method:string;reference:string}[];id:string;sessionId:string;total:string;note:string;createdAt:string;items:{productId:string;quantity:string;amount:string;stockOperationId:string}[];refunds:{paymentId:string;method:string;accountId:string;amount:string;reference:string;treasuryOperationId:string}[]}
const empty=<T>():Page<T>=>({items:[],total:0,page:0,size:20});
@Component({selector:'erp-sale-returns',standalone:true,imports:[FormsModule,DatePipe],templateUrl:'./pos-sale-returns.html'})
export class PosSaleReturns {
 readonly api=inject(Api);private http=inject(HttpClient);private id='';private generation=0;
 @Output() completed=new EventEmitter<void>();
 @Input() externalBlocked=false;
 @Input({required:true}) set saleId(value:string){if(value!==this.id){this.id=value;this.refresh();}}
 readonly sale=signal<Sale|null>(null);readonly history=signal<Page<Return>>(empty());readonly sessions=signal<Page<PosCashSession>>(empty());readonly operatorId=signal('');readonly pending=signal<ReturnInput|null>(null);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');
 readonly receiptSources=signal<Page<ReceiptSource>>(empty());readonly receiptChoices=signal<Record<string,ReceiptChoice>>({});readonly accounts=signal<Page<TreasuryAccount>>(empty());readonly knownAccounts=signal<Record<string,TreasuryAccount>>({});
 readonly quantities=signal<Record<string,string>>({});readonly refunds=signal<Record<string,{amount:string;reference:string}>>({});sessionId='';note='';confirmed=false;
 constructor(){this.api.posOperator().subscribe({next:v=>this.operatorId.set(v.userId),error:e=>this.fail(e)});}
 blocked(){return this.busy()||!!this.pending()||this.externalBlocked;}
 canLeave(){if(!this.pending())return true;this.error.set('Confirme a devolução pendente antes de sair.');return false;}
 @HostListener('window:beforeunload',['$event']) unload(e:BeforeUnloadEvent){if(this.pending()){e.preventDefault();e.returnValue='';}}
 private fail(e:HttpErrorResponse){this.error.set(errorMessage(e));}
 refresh(){if(this.blocked()||!this.id)return;const id=this.id,generation=++this.generation;this.sale.set(null);this.history.set(empty());this.quantities.set({});this.refunds.set({});this.receiptSources.set(empty());this.receiptChoices.set({});this.sessionId='';this.note='';this.confirmed=false;this.http.get<Sale>(`/api/v1/pos/sales/${id}`).subscribe({next:v=>{if(generation===this.generation){this.sale.set(v);if(v.credit&&this.api.can('receivable:read')){this.loadReceiptSources(0);this.loadAccounts(0);}}},error:e=>this.fail(e)});this.loadHistory(0);this.loadSessions(0);}
 loadHistory(page:number){if(this.blocked())return;const id=this.id,generation=this.generation;this.http.get<Page<Return>>(`/api/v1/pos/sales/${id}/returns`,{params:{page,size:20}}).subscribe({next:v=>{if(generation===this.generation)this.history.set(v);},error:e=>this.fail(e)});}
 loadSessions(page:number){if(this.blocked())return;this.api.posCashSessions('OPEN',page).subscribe({next:v=>{this.sessions.set(v);this.sessionId='';},error:e=>this.fail(e)});}
 own(s:PosCashSession){return s.operatorId===this.operatorId()||this.api.can('pos:supervise');}
 quantity(item:Item){return this.quantities()[item.productId]??'';}
 setQuantity(item:Item,value:string){if(!this.blocked())this.quantities.update(q=>({...q,[item.productId]:value}));}
 refundAmount(p:Payment){return this.refunds()[p.id]?.amount??'';}
 refundReference(p:Payment){return this.refunds()[p.id]?.reference??'';}
 setRefund(p:Payment,field:'amount'|'reference',value:string){if(!this.blocked())this.refunds.update(r=>({...r,[p.id]:{amount:r[p.id]?.amount??'',reference:r[p.id]?.reference??'',[field]:value}}));}
 private number(value:string,scale:number,integers:number){if(!new RegExp(`^\\d{1,${integers}}(\\.\\d{1,${scale}})?$`).test(value))return null;const [a,b='']=value.split('.');return BigInt(a)*10n**BigInt(scale)+BigInt(b.padEnd(scale,'0'));}
 private cents(value:string){return this.number(value,2,17)!;}
 private format(value:bigint){const sign=value<0n?'-':'';const v=value<0n?-value:value;return `${sign}${v/100n}.${(v%100n).toString().padStart(2,'0')}`;}
 itemAmount(item:Item):bigint|null{const text=this.quantity(item);if(!text)return 0n;const q=this.number(text,6,13),sold=this.number(item.quantity,6,13)!,previous=this.number(item.returnedQuantity,6,13)!;if(q===null||q<=0n||q+previous>sold)return null;return (2n*this.cents(item.total)*(previous+q)+sold)/(2n*sold)-this.cents(item.returnedAmount);}
 total(){let sum=0n;for(const item of this.sale()?.items??[]){const amount=this.itemAmount(item);if(amount===null)return 'Quantidade inválida';sum+=amount;}return this.format(sum);}
 remaining(p:Payment){return this.format(this.cents(p.amount)-this.cents(p.refundedAmount));}
 debtReduction(){const total=this.total(),s=this.sale();if(total==='Quantidade inválida')return total;return this.format(s?.credit?(this.cents(total)<this.cents(s.credit.outstanding)?this.cents(total):this.cents(s.credit.outstanding)):0n);}
 refundRequired(){const total=this.total(),debt=this.debtReduction();return total==='Quantidade inválida'?total:this.format(this.cents(total)-this.cents(debt));}
 loadReceiptSources(page:number){const s=this.sale();if(this.blocked()||!s?.credit||!this.api.can('receivable:read'))return;const generation=this.generation;this.http.get<Page<ReceiptSource>>(`/api/v1/pos/sales/${s.id}/returns/credit-receipts`,{params:{page,size:20}}).subscribe({next:v=>{if(generation===this.generation)this.receiptSources.set(v);},error:e=>this.fail(e)});}
 loadAccounts(page:number){if(this.blocked())return;this.api.treasuryAccounts(page).subscribe({next:v=>{this.accounts.set(v);this.knownAccounts.update(old=>({...old,...Object.fromEntries(v.items.map(a=>[a.id,a]))}));},error:e=>this.fail(e)});}
 accountOnPage(id:string){return this.accounts().items.some(a=>a.id===id);}
 choice(s:ReceiptSource):ReceiptChoice{return this.receiptChoices()[s.id]??{source:s,amount:'',method:s.accountKind==='CASH'?'CASH':s.accountKind==='BANK'?(s.method==='PIX'?'PIX':'BANK_TRANSFER'):['PIX','BANK_TRANSFER'].includes(s.method)?s.method:'CASH',accountId:s.accountKind==='BANK'?s.treasuryAccountId??'':'',reference:''};}
 setReceipt(s:ReceiptSource,field:'amount'|'method'|'accountId'|'reference',value:string){if(this.blocked())return;this.receiptChoices.update(v=>({...v,[s.id]:{...this.choice(s),[field]:value,...(field==='method'&&value==='CASH'?{reference:'',accountId:''}:{})}}));}
 removeReceipt(id:string){if(!this.blocked())this.receiptChoices.update(v=>{const copy={...v};delete copy[id];return copy;});}
 selectedReceipts(){return Object.values(this.receiptChoices()).filter(c=>!!c.amount);}
 selectedRefundTotal(){let sum=0n;for(const p of this.sale()?.payments??[]){const value=this.refundAmount(p);if(!value)continue;const amount=this.number(value,2,17);if(amount===null||amount<=0n)return 'Valor inválido';sum+=amount;}for(const c of this.selectedReceipts()){const amount=this.number(c.amount,2,17);if(amount===null||amount<=0n)return 'Valor inválido';sum+=amount;}return this.format(sum);}
 label(method:string){return ({CASH:'Dinheiro',PIX:'Pix conferido',BANK_TRANSFER:'Transferência conferida'} as Record<string,string>)[method]??method;}
 productLabel(id:string){const item=this.sale()?.items.find(i=>i.productId===id);return item?`${item.sku} · ${item.name} · ${item.unit}`:'Item da venda';}
 canReturn(){const s=this.sale();return !!s&&s.status==='COMPLETED'&&s.items.some(i=>this.number(i.returnedQuantity,6,13)!<this.number(i.quantity,6,13)!)&&(!s.credit||(this.api.can('receivable:read')&&this.api.can('receivable:credit')))&&this.api.can('pos:return')&&this.api.can('treasury:move');}
 private refundsValid(){
  for(const p of this.sale()?.payments??[]){const text=this.refundAmount(p);if(!text)continue;const amount=this.number(text,2,17);if(amount===null||amount<=0n||amount>this.cents(this.remaining(p)))return false;if(p.method!=='CASH'&&(!this.confirmed||!this.refundReference(p).trim()))return false;}
  for(const c of this.selectedReceipts()){const amount=this.number(c.amount,2,17);if(amount===null||amount<=0n||amount>this.cents(c.source.refundable))return false;if(c.method!=='CASH'&&(!this.confirmed||!c.accountId||!c.reference.trim()))return false;}
  return true;
 }
 valid(){const s=this.sale();return this.refundsValid()&&this.canReturn()&&!!s&&s.items.some(i=>!!this.quantity(i))&&this.total()!=='Quantidade inválida'&&this.selectedRefundTotal()===this.refundRequired()&&this.note.trim().length>=5&&this.sessions().items.some(c=>c.id===this.sessionId&&c.branchId===s.branchId&&this.own(c));}
 submit(){const s=this.sale();if(this.blocked()||!s||!this.valid())return;this.pending.set({requestId:crypto.randomUUID(),sessionId:this.sessionId,version:s.version,...(s.credit?{expectedDebtReduction:this.debtReduction(),receiptRefunds:this.selectedReceipts().map(c=>({receiptId:c.source.id,amount:c.amount,method:c.method,accountId:c.method==='CASH'?null:c.accountId||null,reference:c.reference.trim()}))}:{}),note:this.note.trim(),externalRefundsConfirmed:this.confirmed,items:s.items.filter(i=>!!this.quantity(i)).map(i=>({productId:i.productId,quantity:this.quantity(i)})),refunds:s.payments.filter(p=>!!this.refundAmount(p)).map(p=>({paymentId:p.id,amount:this.refundAmount(p),reference:this.refundReference(p).trim()}))});this.send();}
 send(){const input=this.pending();if(!input||this.busy())return;this.busy.set(true);this.api.inventoryPending.set(true);this.error.set('');this.http.post<Return>(`/api/v1/pos/sales/${this.id}/returns`,input).subscribe({next:r=>{this.busy.set(false);this.pending.set(null);this.api.inventoryPending.set(false);this.message.set(`Devolução de R$ ${r.total} registrada. Abatimento da dívida: R$ ${r.debtReduction}.`);this.refresh();this.completed.emit();},error:(e:HttpErrorResponse)=>{this.busy.set(false);this.fail(e);if(e.status!==0&&e.status<500){this.pending.set(null);this.api.inventoryPending.set(false);if(e.status===409)this.refresh();}else this.message.set('Resultado incerto. Confirme o mesmo comando para evitar repetição.');}});}
}
