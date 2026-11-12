import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Api, Branch, Page, errorMessage } from './api';

@Component({ standalone: true, imports: [ReactiveFormsModule], template: `
  <p class="eyebrow">ESTRUTURA DA EMPRESA</p><h1>Filiais</h1><p>Organize os estabelecimentos da sua operação.</p>
  @if (api.can('branch:write')) { <section class="card"><h2>Nova filial</h2><form class="inline-form" [formGroup]="form" (ngSubmit)="create()"><label>Código<input formControlName="code" maxlength="30" required></label><label>Nome da filial<input formControlName="name" maxlength="160" required></label><button [disabled]="form.invalid || saving()">{{saving() ? 'Salvando…' : 'Cadastrar filial'}}</button></form></section> }
  @if (error()) { <p class="error" role="alert">{{error()}}</p> }
  @if (message()) { <p class="success" role="status">{{message()}}</p> }
  <section class="card"><div class="section-header"><h2>Estabelecimentos</h2><button class="secondary" (click)="load(page())" [disabled]="loading()">Atualizar</button></div>
  @if (loading()) { <p role="status">Carregando filiais…</p> } @else {
  <div class="table-scroll"><table><thead><tr><th>Código</th><th>Nome</th><th>Situação</th></tr></thead><tbody>@for (branch of data().items; track branch.id) { <tr><td class="mono">{{branch.code}}</td><td>{{branch.name}}</td><td><span class="badge">{{branch.active ? 'Ativa' : 'Inativa'}}</span></td></tr> } @empty { <tr><td colspan="3">Nenhuma filial cadastrada.</td></tr> }</tbody></table></div> }
  <div class="pagination"><span>{{data().total}} filiais · Página {{page()+1}}</span><button class="secondary" (click)="load(page()-1)" [disabled]="page()===0 || loading()">Anterior</button><button class="secondary" (click)="load(page()+1)" [disabled]="(page()+1)*20>=data().total || loading()">Próxima</button></div></section>` })
export class Branches {
  readonly api = inject(Api); private fb = inject(FormBuilder);
  readonly data = signal<Page<Branch>>({items: [], total: 0, page: 0, size: 20});
  readonly loading = signal(false); readonly saving = signal(false); readonly error = signal(''); readonly message = signal(''); readonly page = signal(0);
  readonly form = this.fb.nonNullable.group({ code: ['', [Validators.required, Validators.pattern('[A-Za-z0-9_-]{1,30}')]], name: ['', [Validators.required, Validators.maxLength(160)]] });
  constructor() { this.load(0); }
  load(page: number) {
    this.loading.set(true); this.error.set('');
    this.api.branches(page).pipe(finalize(() => this.loading.set(false))).subscribe({ next: data => { this.data.set(data); this.page.set(page); }, error: e => this.error.set(errorMessage(e)) });
  }
  create() {
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true); this.error.set(''); this.message.set('');
    this.api.createBranch(this.form.getRawValue()).pipe(finalize(() => this.saving.set(false))).subscribe({ next: () => { this.form.reset(); this.message.set('Filial cadastrada.'); this.load(0); }, error: e => this.error.set(errorMessage(e)) });
  }
}
