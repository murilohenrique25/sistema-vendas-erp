import { Component, HostListener, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Api, Customer, Page, errorMessage } from './api';
interface Policy {customerId:string;creditLimit:string;blocked:boolean;version:number}
interface Command {id:string;input:{creditLimit:string;blocked:boolean;version:number}}
@Component({standalone:true,imports:[FormsModule],template:`
<p class="eyebrow">COMERCIAL</p><h1>Crédito de clientes</h1><p>Configure o limite para vendas a prazo. Esta administração exige acesso a todas as filiais.</p>
@if(error()){<p class="error" role="alert">{{error()}}</p>}@if(message()){<p class="success" role="status">{{message()}}</p>}
@if(pending()){<section class="card"><h2>Confirmação pendente</h2><p>Repita a mesma configuração para confirmar o resultado antes de sair.</p><button (click)="send()" [disabled]="busy()">Repetir configuração</button></section>}
<section class="card"><h2>Escolher cliente</h2><form class="search-form" (ngSubmit)="load(0)"><label>Buscar<input name="query" [(ngModel)]="query" maxlength="200" [disabled]="blocked()"></label><button [disabled]="blocked()||loading()">Buscar</button></form>
<div class="row-actions">@for(c of customers().items;track c.id){<button class="secondary" (click)="choose(c)" [disabled]="blocked()||loading()">{{c.code}} · {{c.name}} {{c.active?'':'(inativo)'}}</button>}</div>
<div class="pagination"><span>Página {{customers().page+1}}</span><button (click)="load(customers().page-1)" [disabled]="blocked()||loading()||customers().page===0">Anterior</button><button (click)="load(customers().page+1)" [disabled]="blocked()||loading()||(customers().page+1)*20>=customers().total">Próxima</button></div></section>
@if(selected();as c){@if(policy();as p){<section class="card"><h2>{{c.name}}</h2><p>O padrão é bloqueado, com limite zero. O limite considera todos os títulos pendentes do cliente na empresa, inclusive manuais. Cliente com títulos vencidos não recebe novo crédito.</p>
<label>Limite em R$<input [(ngModel)]="limit" inputmode="decimal" [disabled]="blocked()"></label><label><input type="checkbox" [(ngModel)]="creditBlocked" [disabled]="blocked()">Bloquear novas vendas a prazo</label>
<p>Alterar a política não modifica dívidas já existentes. Use ponto decimal.</p><button (click)="save()" [disabled]="blocked()||!valid()">Salvar política de crédito</button><button class="secondary" (click)="choose(c)" [disabled]="blocked()||loading()">Atualizar política</button></section>}}
`})
export class CustomerCredit {
 readonly api=inject(Api);private http=inject(HttpClient);readonly customers=signal<Page<Customer>>({items:[],total:0,page:0,size:20});readonly selected=signal<Customer|null>(null);readonly policy=signal<Policy|null>(null);readonly pending=signal<Command|null>(null);readonly busy=signal(false);readonly loading=signal(false);readonly error=signal('');readonly message=signal('');query='';limit='0';creditBlocked=true;private sequence=0;
 constructor(){if(this.api.session()?.allBranches)this.load(0);else this.error.set('A configuração de crédito exige acesso a todas as filiais.');}
 blocked(){return this.busy()||!!this.pending();}
 @HostListener('window:beforeunload',['$event']) unload(e:BeforeUnloadEvent){if(this.pending()){e.preventDefault();e.returnValue='';}}
 canLeave(){if(!this.pending())return true;this.error.set('Confirme a configuração pendente antes de sair.');return false;}
 load(page:number){if(this.blocked()||this.loading()||!this.api.session()?.allBranches)return;this.loading.set(true);this.api.customers(this.query,page).subscribe({next:v=>{this.customers.set(v);this.loading.set(false);},error:e=>{this.error.set(errorMessage(e));this.loading.set(false);}});}
 choose(c:Customer){if(this.blocked()||this.loading())return;const seq=++this.sequence;this.selected.set(c);this.policy.set(null);this.error.set('');this.loading.set(true);this.http.get<Policy>(`/api/v1/customers/${c.id}/credit-policy`).subscribe({next:p=>{if(seq===this.sequence){this.policy.set(p);this.limit=p.creditLimit;this.creditBlocked=p.blocked;this.loading.set(false);}},error:e=>{if(seq===this.sequence){this.error.set(errorMessage(e));this.loading.set(false);}}});}
 valid(){return /^\d{1,17}(\.\d{1,2})?$/.test(this.limit);}
 save(){const c=this.selected(),p=this.policy();if(this.blocked()||!c||!p||!this.valid())return;this.pending.set({id:c.id,input:{version:p.version,creditLimit:this.limit,blocked:this.creditBlocked}});this.send();}
 send(){const cmd=this.pending();if(!cmd||this.busy())return;this.busy.set(true);this.api.inventoryPending.set(true);this.error.set('');this.message.set('');this.http.put<Policy>(`/api/v1/customers/${cmd.id}/credit-policy`,cmd.input).subscribe({next:p=>{this.policy.set(p);this.limit=p.creditLimit;this.creditBlocked=p.blocked;this.pending.set(null);this.busy.set(false);this.api.inventoryPending.set(false);this.message.set('Política de crédito salva.');},error:(e:HttpErrorResponse)=>{this.busy.set(false);this.error.set(errorMessage(e));if(e.status!==0&&e.status<500){this.pending.set(null);this.api.inventoryPending.set(false);}else this.message.set('Resultado incerto. Repita a configuração preservada.');}});}
}
