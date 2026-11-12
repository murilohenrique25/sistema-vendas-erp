import {Component,signal} from '@angular/core';

import {FormsModule} from '@angular/forms';

import {ActivatedRoute} from '@angular/router';

import {inject} from '@angular/core';

import {errorMessage} from './api';

import {WorkflowClient,WorkflowPage} from './workflow-page';

@Component({standalone:true,imports:[FormsModule],providers:[WorkflowClient],template:`<h1>Devolução ao fornecedor</h1>@if(op.error()){<p class="error">{{op.error()}}</p>}@if(op.pending()){<button (click)="op.send()" [disabled]="op.busy()">Repetir mesma operação</button>}<section class="card"><label>Identificador do recebimento<input [(ngModel)]="receiptId" [disabled]="op.blocked()"></label><button (click)="load()" [disabled]="op.blocked()">Consultar recebimento</button><p>O recebimento é exibido no histórico de compras. Devoluções retiram mercadorias do depósito original e abatem parcelas em aberto. Valores já pagos geram crédito a reclamar do fornecedor; nenhuma entrada bancária é criada. Sem títulos gerados, a devolução reduz o valor da futura geração financeira.</p></section>@if(detail()){<section class="card"><h2>Itens recebidos</h2><table><thead><tr><th>Produto</th><th>Recebido</th><th>Já devolvido</th><th>Devolver agora</th></tr></thead><tbody>@for(l of detail().items;track l.order_line_id){<tr><td>{{l.sku}} · {{l.name}} · {{l.unit}}</td><td>{{l.quantity}}</td><td>{{l.returned_quantity}}</td><td><input [(ngModel)]="quantities[l.order_line_id]" [disabled]="op.blocked()" inputmode="decimal"></td></tr>}</tbody></table><label>Justificativa<input [(ngModel)]="note" [disabled]="op.blocked()" maxlength="500"></label>@if(api.can('purchase:write')&&api.can('inventory:write')&&api.can('payable:write')){<button (click)="preview()" [disabled]="op.blocked()">Conferir valores e ajuste financeiro</button>@if(currentPreview();as p){<p>Valor histórico a devolver: R$ {{p.amount}} · Abatimento de dívida: R$ {{p.debtCredit}} · Crédito a reclamar do fornecedor: R$ {{p.supplierCredit}}</p>@if(!p.financed){<p>O valor devolvido será deduzido da futura geração de contas a pagar.</p>}<button (click)="submit()" [disabled]="op.blocked()">Confirmar itens e valores conferidos</button>}}<h2>Histórico</h2>@for(r of detail().returns;track r.id){<p>{{r.created_at}} · Valor {{r.amount}} · Abatimento {{r.debt_credit}} · Crédito ao fornecedor {{r.supplier_credit}} · {{r.note}}</p>}</section>}`})

export class SupplierReturns extends WorkflowPage {

 readonly route=inject(ActivatedRoute);readonly detail=signal<any>(null);receiptId=this.route.snapshot.queryParamMap.get('receipt')??'';loadedId='';quantities:Record<string,string>={};note='';

 constructor(){super();if(this.receiptId)this.load();}

 load(){if(this.op.blocked()||!this.receiptId)return;const id=this.receiptId;this.op.http.get<any>('/api/v1/supplier-returns/receipt/'+encodeURIComponent(id)).subscribe({next:v=>{if(this.receiptId!==id)return;this.loadedId=id;this.checked.set(null);this.detail.set(v);this.quantities={};},error:e=>{this.detail.set(null);this.op.error.set(errorMessage(e));}});}

 readonly checked=signal<any>(null);private previewSequence=0;
 draft(){if(this.loadedId!==this.receiptId||!this.detail())return null;const items=[];for(const l of this.detail().items){const q=this.quantities[l.order_line_id];if(!q||Number(q)===0)continue;if(!/^\d{1,13}(\.\d{1,6})?$/.test(q)||Number(q)<=0)return null;items.push({orderLineId:l.order_line_id,quantity:q});}if(!items.length||this.note.trim().length<5)return null;return {receiptId:this.loadedId,note:this.note,items};}
 currentPreview(){const p=this.checked();return p&&JSON.stringify(this.draft())===p.key?p.value:null;}
 preview(){const draft=this.draft();if(this.op.blocked()||!draft){this.op.error.set('Informe itens, quantidades e justificativa.');return;}const key=JSON.stringify(draft),sequence=++this.previewSequence;this.checked.set(null);this.op.http.post<any>('/api/v1/supplier-returns/preview',{requestId:crypto.randomUUID(),...draft}).subscribe({next:v=>{if(sequence===this.previewSequence&&JSON.stringify(this.draft())===key)this.checked.set({key,value:v});},error:e=>this.op.error.set(errorMessage(e))});}
 submit(){const draft=this.draft(),p=this.currentPreview();if(this.op.blocked()||!draft||!p)return;this.op.execute('POST','/api/v1/supplier-returns',{requestId:crypto.randomUUID(),...draft,expectedAmount:p.amount,expectedDebtCredit:p.debtCredit,expectedSupplierCredit:p.supplierCredit},()=>{this.checked.set(null);this.note='';this.load();});}
}
