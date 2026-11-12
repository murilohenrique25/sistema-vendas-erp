import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Api, Page, Customer, errorMessage } from './api';

@Component({standalone:true,imports:[ReactiveFormsModule],template:`
  <p class="eyebrow">COMERCIAL</p><h1>Clientes</h1><p>Organize os clientes da sua empresa.</p>
  @if (error()) { <p class="error" role="alert">{{error()}}</p> } @if (message()) { <p class="success" role="status">{{message()}}</p> }
  @if (api.can('customer:write')) { <section class="card"><h2>{{editing() ? 'Editar cliente' : 'Novo cliente'}}</h2>
  <form [formGroup]="form" (ngSubmit)="save()"><div class="product-form">
    <label>Código<input formControlName="code" maxlength="40" required></label><label>Nome / razão social<input formControlName="name" maxlength="200" required></label>
    <label>Identificação fiscal informada<input formControlName="taxIdentifier" maxlength="40"></label><label>Pessoa de contato<input formControlName="contactName" maxlength="160"></label>
    <label>E-mail<input type="email" formControlName="email" maxlength="254"></label><label>Telefone<input formControlName="phone" maxlength="40"></label>
    <label>Endereço<textarea formControlName="address" maxlength="500" rows="3"></textarea></label><label>Observações<textarea formControlName="notes" maxlength="2000" rows="3"></textarea></label>
  </div>@if (editing()) { <fieldset class="permissions" [disabled]="saving()"><legend>Situação</legend><label><input type="checkbox" [checked]="active()" (change)="active.set(!active())">Cliente ativo</label></fieldset> }
  <div class="row-actions"><button [disabled]="form.invalid || saving()">{{saving() ? 'Salvando…' : 'Salvar cliente'}}</button>@if (editing()) { <button type="button" class="secondary" (click)="reset()" [disabled]="saving()">Cancelar edição</button> }</div></form></section> }
  <section class="card"><div class="section-header"><h2>Clientes cadastrados</h2><button class="secondary" (click)="load(page())" [disabled]="saving() || loading()">Atualizar</button></div>
  <form class="search-form" [formGroup]="search" (ngSubmit)="load(0)"><label>Buscar por nome, código ou identificação<input formControlName="q" maxlength="200"></label><button [disabled]="search.invalid || loading() || saving()">Buscar</button></form>
  @if (loading()) { <p role="status">Carregando clientes…</p> } @else {
  <div class="table-scroll"><table><thead><tr><th>Código / nome</th><th>Contato</th><th>Situação</th><th>Ações</th></tr></thead><tbody>
  @for (customer of data().items; track customer.id) { <tr><td><span class="mono">{{customer.code}}</span><br>{{customer.name}}</td><td>{{customer.contactName}}<br>{{customer.email}}<br>{{customer.phone}}</td><td>{{customer.active ? 'Ativo' : 'Inativo'}}</td><td>
  @if (api.can('customer:write')) { <div class="row-actions"><button class="secondary" (click)="edit(customer)" [disabled]="saving()">Editar</button><button class="secondary" (click)="toggleActive(customer)" [disabled]="saving()">{{customer.active ? 'Desativar' : 'Reativar'}}</button></div> }
  </td></tr> } @empty { <tr><td colspan="4">Nenhum cliente encontrado.</td></tr> }</tbody></table></div> }
  <div class="pagination"><span>{{data().total}} clientes · Página {{page()+1}}</span><button class="secondary" (click)="load(page()-1)" [disabled]="page()===0 || loading() || saving()">Anterior</button><button class="secondary" (click)="load(page()+1)" [disabled]="(page()+1)*20>=data().total || loading() || saving()">Próxima</button></div></section>`})
export class Customers {
  readonly api=inject(Api);private fb=inject(FormBuilder);
  readonly data=signal<Page<Customer>>({items:[],total:0,page:0,size:20});readonly page=signal(0);readonly loading=signal(false);readonly saving=signal(false);
  readonly error=signal('');readonly message=signal('');readonly editing=signal<Customer|null>(null);readonly active=signal(true);
  readonly search=this.fb.nonNullable.group({q:['',Validators.maxLength(200)]});
  readonly form=this.fb.nonNullable.group({
    code:['',[Validators.required,Validators.pattern('[A-Za-z0-9._-]{1,40}')]],name:['',[Validators.required,Validators.pattern('.*\\S.*'),Validators.maxLength(200)]],
    taxIdentifier:['',Validators.maxLength(40)],contactName:['',Validators.maxLength(160)],email:['',[Validators.email,Validators.maxLength(254)]],phone:['',Validators.maxLength(40)],address:['',Validators.maxLength(500)],notes:['',Validators.maxLength(2000)]
  });
  constructor() { this.load(0); }
  private input(customer:Customer) { const {id,active,version,...data}=customer;return data; }
  reset() { this.editing.set(null);this.form.reset();this.active.set(true); }
  edit(customer:Customer) { this.editing.set(customer);this.form.setValue(this.input(customer));this.active.set(customer.active);this.error.set('');this.message.set(''); }
  load(page:number) {
    if(this.search.invalid)return;
    this.loading.set(true);this.error.set('');this.api.customers(this.search.controls.q.value,page).pipe(finalize(()=>this.loading.set(false))).subscribe({next:data=>{this.data.set(data);this.page.set(page);},error:e=>this.error.set(errorMessage(e))});
  }
  save() {
    if(this.form.invalid || this.saving())return;
    const customer=this.editing(),input=this.form.getRawValue();this.saving.set(true);this.error.set('');this.message.set('');
    (customer?this.api.updateCustomer(customer,input,this.active()):this.api.createCustomer(input)).pipe(finalize(()=>this.saving.set(false))).subscribe({next:()=>{this.reset();this.message.set('Cliente salvo.');this.load(0);},error:e=>this.error.set(errorMessage(e))});
  }
  toggleActive(customer:Customer) {
    if(this.saving())return;
    this.saving.set(true);this.error.set('');this.message.set('');
    this.api.updateCustomer(customer,this.input(customer),!customer.active).pipe(finalize(()=>this.saving.set(false))).subscribe({next:()=>{if(this.editing()?.id===customer.id)this.reset();this.message.set(customer.active?'Cliente desativado.':'Cliente reativado.');this.load(this.page());},error:e=>this.error.set(errorMessage(e))});
  }
}
