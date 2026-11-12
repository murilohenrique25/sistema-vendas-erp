import { Component, EventEmitter, HostListener, Input, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { Api, Payable, errorMessage } from './api';
interface Summary {receiptId:string;branchId:string;supplierId:string;amount:string;linkedTitleCount:number;generatedTitle:Payable|null;generatedTitles:Payable[]}
interface FinanceInput {requestId:string;reference:string;document:string;description:string;issuedOn:string;dueOn:string;installmentCount:number;intervalMonths:number}
interface InstallmentPreview {amount:string;installments:{position:number;reference:string;dueOn:string;amount:string}[]}
@Component({selector:'erp-purchase-receipt-finance',standalone:true,imports:[FormsModule,RouterLink],templateUrl:'./purchase-receipt-finance.html'})
export class PurchaseReceiptFinance {
 private readonly http=inject(HttpClient);readonly api=inject(Api);private source:{orderId:string;receiptId:string}|null=null;private sequence=0;
 readonly summary=signal<Summary|null>(null);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');readonly pending=signal<{url:string;input:FinanceInput}|null>(null);
 @Input() externalBlocked=false;@Output() closed=new EventEmitter<void>();
 @Input() set receipt(value:{orderId:string;receiptId:string}|null){if(this.pending()||this.busy())return;this.source=value;this.summary.set(null);this.message.set('');this.error.set('');if(value)this.load();}
 readonly installmentPreview=signal<{key:string;value:InstallmentPreview}|null>(null);installmentCount=1;intervalMonths=1;
 reference='';document='';description='';issuedOn='';dueOn='';
 private url(){return `/api/v1/purchase-orders/${this.source!.orderId}/receipts/${this.source!.receiptId}/payable`;}
 blocked(){return this.busy()||!!this.pending()||this.externalBlocked;}
 canLeave(){return !this.pending()&&!this.busy();}
 @HostListener('window:beforeunload',['$event']) unload(e:BeforeUnloadEvent){if(this.pending()){e.preventDefault();e.returnValue='';}}
 load(){if(!this.source||this.blocked())return;const sequence=++this.sequence;this.busy.set(true);forkJoin({summary:this.http.get<Summary>(this.url()),calendar:this.api.payableCalendar()}).subscribe({next:v=>{if(sequence!==this.sequence)return;this.busy.set(false);this.summary.set(v.summary);this.issuedOn=v.calendar.date;this.dueOn='';this.reference=`PUR-${v.summary.receiptId.replaceAll('-','')}`;this.document='';this.description='Mercadorias recebidas no pedido de compra';this.installmentCount=1;this.intervalMonths=1;this.installmentPreview.set(null);},error:e=>{if(sequence===this.sequence){this.busy.set(false);this.error.set(errorMessage(e));}}});}
 close(){if(!this.blocked()){this.source=null;this.summary.set(null);this.closed.emit();}}
 private formData(requestId:string):FinanceInput{return {requestId,reference:this.reference,document:this.document.trim(),description:this.description.trim(),issuedOn:this.issuedOn,dueOn:this.dueOn,installmentCount:this.installmentCount,intervalMonths:this.intervalMonths};}
 private previewKey(){return JSON.stringify({input:this.formData(''),amount:this.summary()?.amount});}
 currentPreview(){const preview=this.installmentPreview();return preview?.key===this.previewKey()?preview.value:null;}
 private validForm(){return /^[A-Za-z0-9._-]{1,60}$/.test(this.reference)&&/^\d{4}-\d{2}-\d{2}$/.test(this.issuedOn)&&/^\d{4}-\d{2}-\d{2}$/.test(this.dueOn)&&this.dueOn>=this.issuedOn&&this.description.trim().length>=5&&this.description.trim().length<=500&&this.document.length<=160&&Number.isInteger(this.installmentCount)&&this.installmentCount>=1&&this.installmentCount<=120&&Number.isInteger(this.intervalMonths)&&this.intervalMonths>=1&&this.intervalMonths<=12&&(this.installmentCount===1||this.reference.length<=56);}
 preview(){const summary=this.summary();if(this.blocked()||!summary||summary.generatedTitle||summary.linkedTitleCount>0)return;if(!this.validForm()){this.error.set('Confira datas, referência (até 56 caracteres se parcelado), descrição e quantidade/intervalo das parcelas.');return;}const key=this.previewKey();this.busy.set(true);this.error.set('');this.installmentPreview.set(null);this.http.post<InstallmentPreview>(this.url()+'/preview',this.formData(crypto.randomUUID())).subscribe({next:v=>{this.busy.set(false);this.installmentPreview.set({key,value:v});},error:e=>{this.busy.set(false);this.error.set(errorMessage(e));}});}
 create(){const summary=this.summary();if(this.blocked()||!summary||summary.generatedTitle||summary.linkedTitleCount>0)return;if(!this.validForm()||!this.currentPreview()){this.error.set('Confira as parcelas atuais antes de gerar as contas a pagar.');return;}this.pending.set({url:this.url(),input:this.formData(crypto.randomUUID())});this.send();}

 send(){const p=this.pending();if(!p||this.busy()||this.externalBlocked)return;this.busy.set(true);this.api.inventoryPending.set(true);this.error.set('');this.http.post<Summary>(p.url,p.input).subscribe({next:v=>{this.busy.set(false);this.pending.set(null);this.api.inventoryPending.set(false);this.summary.set(v);this.message.set('Contas a pagar geradas e vinculadas ao recebimento. Nenhum pagamento foi realizado.');},error:(e:HttpErrorResponse)=>{this.busy.set(false);this.error.set(errorMessage(e));if(e.status!==0&&e.status<500){this.installmentPreview.set(null);this.pending.set(null);this.api.inventoryPending.set(false);}else this.message.set('Resultado incerto. Repita o mesmo comando para conferir a geração sem duplicar o título.');}});}
}
