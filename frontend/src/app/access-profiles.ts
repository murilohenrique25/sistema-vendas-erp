import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Api, AccessProfile, Page, errorMessage } from './api';
import { permissionLabels } from './permission-labels';

@Component({standalone:true,imports:[ReactiveFormsModule],template:`
  <p class="eyebrow">ACESSOS DA EMPRESA</p><h1>Perfis de acesso</h1><p>Reutilize conjuntos de permissões. Aplique um perfil pela edição de permissões de um usuário. Alterar o perfil não modifica os usuários automaticamente.</p>
  @if (error()) { <p class="error" role="alert">{{error()}}</p> } @if (message()) { <p class="success" role="status">{{message()}}</p> }
  @if (api.can('user:write')) { <section class="card"><h2>{{editing() ? 'Editar perfil' : 'Novo perfil'}}</h2>
  <form [formGroup]="form" (ngSubmit)="save()"><label>Nome<input formControlName="name" maxlength="160" required></label>
  <fieldset class="permissions" [disabled]="saving()"><legend>Permissões</legend>
  @for (permission of available(); track permission) { <label><input type="checkbox" [checked]="selected().includes(permission)" (change)="toggle(permission)">{{label(permission)}}</label> }
  </fieldset><p>Administrar usuários exige também consultar usuários.</p><div class="row-actions"><button [disabled]="form.invalid || saving()">Salvar perfil</button>@if (editing()) { <button type="button" class="secondary" (click)="reset()" [disabled]="saving()">Cancelar edição</button> }</div></form></section> }
  <section class="card"><div class="section-header"><h2>Perfis cadastrados</h2><button class="secondary" (click)="load(page())" [disabled]="saving() || loading()">Atualizar</button></div>
  @if (loading()) { <p role="status">Carregando perfis…</p> } @else {
  <div class="table-scroll"><table><thead><tr><th>Nome</th><th>Permissões</th><th>Ações</th></tr></thead><tbody>
  @for (profile of data().items; track profile.id) { <tr><td>{{profile.name}}</td><td>{{labels(profile.permissions)}}</td><td>@if (api.can('user:write')) { <button class="secondary" (click)="edit(profile)" [disabled]="saving() || !canEdit(profile)">Editar</button> }</td></tr> } @empty { <tr><td colspan="3">Nenhum perfil cadastrado.</td></tr> }
  </tbody></table></div> }
  <div class="pagination"><span>{{data().total}} perfis · Página {{page()+1}}</span><button class="secondary" (click)="load(page()-1)" [disabled]="page()===0 || loading() || saving()">Anterior</button><button class="secondary" (click)="load(page()+1)" [disabled]="(page()+1)*20>=data().total || loading() || saving()">Próxima</button></div></section>`})
export class AccessProfiles {
  readonly api=inject(Api);private fb=inject(FormBuilder);
  readonly data=signal<Page<AccessProfile>>({items:[],total:0,page:0,size:20});readonly page=signal(0);readonly loading=signal(false);readonly saving=signal(false);
  readonly error=signal('');readonly message=signal('');readonly selected=signal<string[]>([]);readonly editing=signal<AccessProfile|null>(null);
  readonly form=this.fb.nonNullable.group({name:['',[Validators.required,Validators.pattern('.*\\S.*'),Validators.maxLength(160)]]});
  constructor() { this.load(0); }
  available() { return Object.keys(permissionLabels).filter(permission=>this.api.can(permission)); }
  label(permission:string) { return permissionLabels[permission] ?? permission; }
  labels(permissions:string[]) { return permissions.map(permission=>this.label(permission)).join(', ') || 'Sem acesso aos módulos'; }
  canEdit(profile:AccessProfile) { return profile.permissions.every(permission=>this.api.can(permission)); }
  toggle(permission:string) { this.selected.update(values=>values.includes(permission)?values.filter(value=>value!==permission):[...values,permission]); }
  reset() { this.editing.set(null);this.form.reset();this.selected.set([]); }
  edit(profile:AccessProfile) { this.editing.set(profile);this.form.setValue({name:profile.name});this.selected.set([...profile.permissions]);this.error.set('');this.message.set(''); }
  load(page:number) { this.loading.set(true);this.error.set('');this.api.accessProfiles(page).pipe(finalize(()=>this.loading.set(false))).subscribe({next:data=>{this.data.set(data);this.page.set(page);},error:e=>this.error.set(errorMessage(e))}); }
  save() {
    if(this.form.invalid || this.saving()) return;
    const input={...this.form.getRawValue(),permissions:this.selected()},profile=this.editing();
    this.saving.set(true);this.error.set('');this.message.set('');
    (profile?this.api.updateAccessProfile(profile,input):this.api.createAccessProfile(input)).pipe(finalize(()=>this.saving.set(false))).subscribe({next:()=>{this.reset();this.message.set('Perfil salvo. Aplique aos usuários para atualizar seus acessos.');this.load(0);},error:e=>this.error.set(errorMessage(e))});
  }
}
