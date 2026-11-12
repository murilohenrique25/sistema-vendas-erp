import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Router } from '@angular/router';
import { Api, Page, User, AccessProfile, Branch, errorMessage } from './api';
import { permissionLabels } from './permission-labels';
@Component({standalone: true, imports: [ReactiveFormsModule], template: `
  <p class="eyebrow">ACESSOS DA EMPRESA</p><h1>Usuários</h1><p>Cadastre pessoas e controle seus acessos à operação.</p>
  @if (error()) { <p class="error" role="alert">{{error()}}</p> }
  @if (message()) { <p class="success" role="status">{{message()}}</p> }
  @if (api.can('user:write')) { <section class="card"><h2>Novo usuário</h2><p>A senha inicial deve ser entregue ao usuário por um canal seguro. Ele pode trocá-la em Segurança da conta.</p>
  <form [formGroup]="form" (ngSubmit)="create()"><div class="inline-form">
    <label>Nome<input formControlName="name" maxlength="160" autocomplete="off" required></label>
    <label>E-mail<input type="email" formControlName="email" maxlength="254" autocomplete="off" required></label>
    <label>Senha inicial<input type="password" formControlName="password" minlength="12" maxlength="72" autocomplete="new-password" required></label>
  </div><p>Senha de 12 a 72 caracteres ASCII (sem acentos). Selecione as permissões; apenas seus próprios privilégios podem ser concedidos.</p>
  <fieldset class="permissions" [disabled]="saving()"><legend>Permissões</legend>
  @for (permission of available(); track permission) { <label><input type="checkbox" [checked]="selected().includes(permission)" (change)="toggle(permission)"> {{label(permission)}}</label> }
  </fieldset><button [disabled]="form.invalid || saving()">{{saving() ? 'Salvando…' : 'Cadastrar usuário'}}</button></form></section> }
  @if (editing(); as user) { <section class="card"><h2>Permissões de {{user.name}}</h2><p>{{user.email}} · Alterações valem para os acessos já abertos. A empresa deve manter um gestor ativo.</p>
  <h3>Acesso às filiais</h3><p>Gestores de usuários precisam de acesso global. Lista restrita vazia bloqueia todas as filiais; catálogo permanece compartilhado pela empresa.</p>
  <fieldset class="permissions" [disabled]="saving() || !scopeReady()"><legend>Escopo</legend><label><input type="checkbox" [checked]="allBranches()" (change)="allBranches.set(!allBranches())">Todas as filiais, inclusive futuras</label>
  @if (!allBranches()) { @for (branch of branchData().items; track branch.id) { <label><input type="checkbox" [checked]="branchIds().includes(branch.id)" (change)="toggleBranch(branch.id)">{{branch.code}} · {{branch.name}}</label> } }
  </fieldset>
  @if (!allBranches()) { <div class="pagination"><span>{{branchIds().length}} selecionadas · Página {{branchPage()+1}}</span><button class="secondary" (click)="loadBranches(branchPage()-1)" [disabled]="branchPage()===0 || saving() || branchLoading()">Anterior</button><button class="secondary" (click)="loadBranches(branchPage()+1)" [disabled]="(branchPage()+1)*20>=branchData().total || saving() || branchLoading()">Próxima</button></div> }
  <button (click)="saveScope()" [disabled]="saving() || !scopeReady() || branchLoading()">Salvar acesso às filiais</button>
  <h3>Aplicar perfil</h3><p>Substitui as permissões deste usuário pelo conjunto atual do perfil. Alterações futuras do perfil precisam ser reaplicadas.</p>
  @if (profilesLoading()) { <p role="status">Carregando perfis…</p> }
  <div class="row-actions">@for (profile of profiles().items; track profile.id) { <button class="secondary" [disabled]="saving() || profilesLoading() || !profile.permissions.every(canPermission)" (click)="applyProfile(profile)">{{profile.name}}</button> } @empty { <p>Nenhum perfil nesta página.</p> }</div>
  <div class="pagination"><span>{{profiles().total}} perfis · Página {{profilePage()+1}}</span><button class="secondary" (click)="loadProfiles(profilePage()-1)" [disabled]="profilePage()===0 || profilesLoading() || saving()">Anterior</button><button class="secondary" (click)="loadProfiles(profilePage()+1)" [disabled]="(profilePage()+1)*20>=profiles().total || profilesLoading() || saving()">Próxima</button></div>
  <fieldset class="permissions" [disabled]="saving()"><legend>Acessos aos módulos</legend>
  @for (permission of available(); track permission) { <label><input type="checkbox" [checked]="editedPermissions().includes(permission)" (change)="toggleEdit(permission)"> {{label(permission)}}</label> }
  </fieldset><div class="row-actions"><button (click)="savePermissions()" [disabled]="saving()">Salvar permissões</button><button class="secondary" (click)="editing.set(null)" [disabled]="saving()">Cancelar</button></div></section> }
  <section class="card"><div class="section-header"><h2>Pessoas cadastradas</h2><button class="secondary" (click)="load(page())" [disabled]="loading() || saving()">Atualizar</button></div>
  @if (loading()) { <p role="status">Carregando usuários…</p> } @else {
  <div class="table-scroll"><table><thead><tr><th>Nome / e-mail</th><th>Permissões</th><th>Situação</th><th>Ações</th></tr></thead><tbody>
  @for (user of data().items; track user.id) { <tr><td>{{user.name}}<br>{{user.email}}</td><td>{{labels(user.permissions)}}</td><td>{{user.active ? 'Ativo' : 'Inativo'}}</td><td>
  @if (api.can('user:write')) { <div class="row-actions"><button class="secondary" [disabled]="saving()" (click)="changeStatus(user)">{{user.active ? 'Desativar' : 'Reativar'}}</button><button class="secondary" [disabled]="saving() || !canEdit(user)" (click)="edit(user)">Permissões</button></div> }
  </td></tr> } @empty { <tr><td colspan="4">Nenhum usuário cadastrado.</td></tr> }
  </tbody></table></div> }
  <p>Desativar encerra todos os acessos dessa pessoa. Sua própria conta não pode ser desativada aqui.</p>
  <div class="pagination"><span>{{data().total}} usuários · Página {{page()+1}}</span><button class="secondary" (click)="load(page()-1)" [disabled]="page()===0 || loading() || saving()">Anterior</button><button class="secondary" (click)="load(page()+1)" [disabled]="(page()+1)*20>=data().total || loading() || saving()">Próxima</button></div></section>`})
export class Users {
  readonly api=inject(Api); private fb=inject(FormBuilder); private router=inject(Router);
  readonly editing=signal<User|null>(null); readonly editedPermissions=signal<string[]>([]);
  readonly allBranches=signal(true);readonly branchIds=signal<string[]>([]);readonly scopeReady=signal(false);
  readonly branchData=signal<Page<Branch>>({items:[],total:0,page:0,size:20});readonly branchPage=signal(0);readonly branchLoading=signal(false);
  readonly profiles=signal<Page<AccessProfile>>({items:[],total:0,page:0,size:20}); readonly profilePage=signal(0); readonly profilesLoading=signal(false);
  readonly canPermission=(permission:string)=>this.api.can(permission);
  readonly data=signal<Page<User>>({items:[],total:0,page:0,size:20}); readonly page=signal(0);
  readonly loading=signal(false); readonly saving=signal(false); readonly error=signal(''); readonly message=signal(''); readonly selected=signal<string[]>([]);
  readonly form=this.fb.nonNullable.group({name:['',[Validators.required,Validators.pattern('.*\\S.*'),Validators.maxLength(160)]],email:['',[Validators.required,Validators.email,Validators.maxLength(254)]],password:['',[Validators.required,Validators.minLength(12),Validators.maxLength(72),Validators.pattern('[\\x20-\\x7E]+')]]});
  constructor() { this.load(0); }
  available() { return Object.keys(permissionLabels).filter(permission=>this.api.can(permission)); }
  label(permission:string) { return permissionLabels[permission] ?? permission; }
  labels(permissions:string[]) { return permissions.map(permission=>this.label(permission)).join(', ') || 'Sem acesso aos módulos'; }
  toggle(permission:string) { this.selected.update(values=>values.includes(permission)?values.filter(value=>value!==permission):[...values,permission]); }
  canEdit(user:User) { return user.permissions.every(permission=>this.api.can(permission)); }
  edit(user:User) {
    this.editing.set(user);this.editedPermissions.set([...user.permissions]);this.error.set('');this.message.set('');this.loadProfiles(0);this.scopeReady.set(false);
    this.api.userBranchScope(user.id).subscribe({next:scope=>{if(this.editing()?.id!==user.id)return;if(scope.version!==user.version){this.error.set('Usuário alterado. Atualize a lista e abra novamente.');return;}this.allBranches.set(scope.allBranches);this.branchIds.set(scope.branchIds);this.scopeReady.set(true);},error:e=>this.error.set(errorMessage(e))});this.loadBranches(0);
  }
  loadBranches(page:number) { this.branchLoading.set(true);this.api.branches(page).pipe(finalize(()=>this.branchLoading.set(false))).subscribe({next:data=>{this.branchData.set(data);this.branchPage.set(page);},error:e=>this.error.set(errorMessage(e))}); }
  toggleBranch(id:string) { this.branchIds.update(ids=>ids.includes(id)?ids.filter(value=>value!==id):[...ids,id]); }
  saveScope() {
    const user=this.editing();if(!user || this.saving() || !this.scopeReady())return;
    this.saving.set(true);this.error.set('');this.message.set('');
    this.api.setUserBranchScope(user,this.allBranches(),this.allBranches()?[]:this.branchIds()).subscribe({next:()=>this.afterPermissionsSaved(),error:e=>{this.saving.set(false);this.error.set(errorMessage(e));}});
  }
  loadProfiles(page:number) { this.profilesLoading.set(true);this.api.accessProfiles(page).pipe(finalize(()=>this.profilesLoading.set(false))).subscribe({next:data=>{this.profiles.set(data);this.profilePage.set(page);},error:e=>this.error.set(errorMessage(e))}); }
  applyProfile(profile:AccessProfile) {
    const user=this.editing();if(!user || this.saving()) return;
    this.saving.set(true);this.error.set('');this.message.set('');
    this.api.applyAccessProfile(user,profile).subscribe({next:()=>this.afterPermissionsSaved(),error:e=>{this.saving.set(false);this.error.set(errorMessage(e));}});
  }
  toggleEdit(permission:string) { this.editedPermissions.update(values=>values.includes(permission)?values.filter(value=>value!==permission):[...values,permission]); }
  savePermissions() {
    const user=this.editing(); if(!user || this.saving()) return;
    this.saving.set(true);this.error.set('');this.message.set('');
    this.api.setUserPermissions(user,this.editedPermissions()).subscribe({next:()=>this.afterPermissionsSaved(),error:e=>{this.saving.set(false);this.error.set(errorMessage(e));}});
  }
  private afterPermissionsSaved() {
      this.editing.set(null);this.message.set('Permissões atualizadas.');
      this.api.me().pipe(finalize(()=>this.saving.set(false))).subscribe({next:me=>{
        const session=this.api.session(); if(session) this.api.session.set({...session,...me});
        if(this.api.can('user:read')) this.load(this.page()); else void this.router.navigateByUrl('/account');
      },error:e=>this.error.set(errorMessage(e))});
  }
  load(page:number) { this.loading.set(true); this.error.set(''); this.api.users(page).pipe(finalize(()=>this.loading.set(false))).subscribe({next:data=>{this.data.set(data);this.page.set(page);},error:e=>this.error.set(errorMessage(e))}); }
  create() {
    if(this.form.invalid || this.saving()) return;
    this.saving.set(true);this.error.set('');this.message.set('');
    this.api.createUser({...this.form.getRawValue(),permissions:this.selected()}).pipe(finalize(()=>this.saving.set(false))).subscribe({next:()=>{this.form.reset();this.selected.set([]);this.message.set('Usuário cadastrado.');this.load(0);},error:e=>{this.form.controls.password.reset();this.error.set(errorMessage(e));}});
  }
  changeStatus(user:User) {
    if(this.saving()) return;
    this.saving.set(true);this.error.set('');this.message.set('');
    this.api.setUserActive(user,!user.active).pipe(finalize(()=>this.saving.set(false))).subscribe({next:()=>{this.message.set(user.active?'Usuário desativado e sessões encerradas.':'Usuário reativado.');this.load(this.page());},error:e=>this.error.set(errorMessage(e))});
  }
}
