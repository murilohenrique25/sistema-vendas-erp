import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { Api, errorMessage } from './api';

@Component({ standalone: true, imports: [ReactiveFormsModule], template: `
  <section class="login"><div class="brand">ERP<span>COMERCIAL</span></div><p class="eyebrow">GESTÃO COM SEGURANÇA</p><h1>Bem-vindo à sua operação.</h1><p>Entre para administrar as filiais da sua empresa.</p>
  <form [formGroup]="form" (ngSubmit)="submit()"><label>Empresa<input formControlName="company" autocomplete="organization" placeholder="identificador-da-empresa" required></label><label>E-mail<input type="email" formControlName="email" autocomplete="username" required></label><label>Senha<input type="password" formControlName="password" autocomplete="current-password" required></label>
  @if (error()) { <p class="error" role="alert">{{error()}}</p> }
  <button [disabled]="form.invalid || busy()">{{busy() ? 'Entrando…' : 'Entrar'}}</button></form><small>O acesso é restrito à empresa e às permissões do usuário.</small></section>` })
export class Login {
  private api = inject(Api); private router = inject(Router); private fb = inject(FormBuilder);
  readonly error = signal(''); readonly busy = signal(false);
  readonly form = this.fb.nonNullable.group({ company: ['', [Validators.required, Validators.pattern('[a-z0-9-]{3,80}')]], email: ['', [Validators.required, Validators.email]], password: ['', Validators.required] });
  submit() {
    if (this.form.invalid || this.busy()) return;
    this.busy.set(true); this.error.set('');
    this.api.login(this.form.getRawValue()).pipe(finalize(() => this.busy.set(false))).subscribe({ next: session => {
      this.api.session.set(session); this.form.controls.password.reset();
      const destination = [['branch:read','/branches'],['product:read','/products'],['supplier:read','/suppliers'],['purchase:read','/purchases'],['pos:read','/pos-cash'],['reconciliation:read','/reconciliation'],['cashflow:read','/cash-flow'],['customer:read','/customers'],['receivable:read','/receivables'],['treasury:read','/treasury'],['payable:read','/payables'],['inventory:read','/inventory'],['loss:read','/losses'],['user:read','/users'],['audit:read','/audit']].find(([permission])=>this.api.can(permission));
      void this.router.navigateByUrl(destination?.[1] ?? '/account');
    }, error: e => this.error.set(errorMessage(e)) });
  }
}



