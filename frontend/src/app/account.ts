import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ActiveSession, Api, errorMessage } from './api';

@Component({ standalone: true, imports: [ReactiveFormsModule, DatePipe], template: `
  <p class="eyebrow">SUA CONTA</p><h1>Segurança</h1>
  @if (error()) { <p class="error" role="alert">{{error()}}</p> }
  @if (message()) { <p class="success" role="status">{{message()}}</p> }
  <section class="card"><div class="section-header"><h2>Sessões ativas</h2><button class="secondary" (click)="load()" [disabled]="busy() || loading()">Atualizar</button></div>
  <p>Os acessos expiram após oito horas. Encerrar outros acessos mantém esta sessão aberta.</p>
  @if (loading()) { <p role="status">Carregando sessões…</p> } @else {
    <div class="table-scroll"><table><thead><tr><th>Acesso</th><th>Início</th><th>Expiração</th></tr></thead><tbody>
    @for (session of sessions(); track $index) { <tr><td>{{session.current ? 'Esta sessão' : 'Outro acesso'}}</td><td>{{session.createdAt | date:'dd/MM/yyyy HH:mm'}}</td><td>{{session.expiresAt | date:'dd/MM/yyyy HH:mm'}}</td></tr> }
    @empty { <tr><td colspan="3">Nenhuma sessão disponível.</td></tr> }
    </tbody></table></div>
  }
  <button (click)="revoke()" [disabled]="busy() || loading() || sessions().length < 2">Encerrar outros acessos</button></section>
  <section class="card"><h2>Trocar senha</h2><p>Use de 12 a 72 caracteres ASCII: letras sem acentos, números, espaços e símbolos. Após a troca, todos os acessos serão encerrados e você fará login novamente.</p>
  <form class="inline-form" [formGroup]="form" (ngSubmit)="changePassword()">
    <label>Senha atual<input type="password" autocomplete="current-password" formControlName="currentPassword" maxlength="72" required></label>
    <label>Nova senha<input type="password" autocomplete="new-password" formControlName="newPassword" minlength="12" maxlength="72" required></label>
    <label>Confirmar nova senha<input type="password" autocomplete="new-password" formControlName="confirmation" maxlength="72" required></label>
    <button [disabled]="form.invalid || busy() || form.controls.newPassword.value !== form.controls.confirmation.value">Trocar senha e sair</button>
  </form></section>` })
export class Account {
  private api = inject(Api); private router = inject(Router); private fb = inject(FormBuilder);
  readonly sessions = signal<ActiveSession[]>([]); readonly loading = signal(false); readonly busy = signal(false);
  readonly error = signal(''); readonly message = signal('');
  readonly form = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required, Validators.maxLength(72)]],
    newPassword: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(72), Validators.pattern('[\\x20-\\x7E]+')]],
    confirmation: ['', Validators.required]
  });
  constructor() { this.load(); }
  load() {
    this.loading.set(true); this.error.set('');
    this.api.activeSessions().pipe(finalize(() => this.loading.set(false))).subscribe({next: value => this.sessions.set(value), error: e => this.error.set(errorMessage(e))});
  }
  revoke() {
    if (this.busy() || this.loading()) return;
    this.busy.set(true); this.error.set(''); this.message.set('');
    this.api.revokeOtherSessions().pipe(finalize(() => this.busy.set(false))).subscribe({next: () => { this.message.set('Outros acessos encerrados.'); this.load(); }, error: e => this.error.set(errorMessage(e))});
  }
  changePassword() {
    const value = this.form.getRawValue();
    if (this.form.invalid || this.busy() || value.newPassword !== value.confirmation) return;
    this.busy.set(true); this.error.set(''); this.message.set('');
    this.api.changePassword({currentPassword: value.currentPassword, newPassword: value.newPassword}).pipe(finalize(() => this.busy.set(false))).subscribe({next: () => {
      this.form.reset(); this.api.session.set(null); void this.router.navigateByUrl('/login');
    }, error: e => { this.form.reset(); this.error.set(errorMessage(e)); }});
  }
}
