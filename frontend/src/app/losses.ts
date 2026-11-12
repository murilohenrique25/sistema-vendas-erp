import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Api, Page, StockLoss, errorMessage } from './api';

@Component({standalone: true, imports: [DatePipe, ReactiveFormsModule], template: `
<p class="eyebrow">GESTÃO DE PERDAS</p><h1>Perdas e aprovações</h1><p>Registros pendentes preservam o saldo. Cada aprovação gera uma única baixa rastreável.</p>
@if (error()) { <p class="error" role="alert">{{error()}}</p> } @if (message()) { <p class="success" role="status">{{message()}}</p> }
@if (rejecting()) { <section class="card"><h2>Justificar rejeição</h2><form [formGroup]="rejection" (ngSubmit)="reject()"><label>Justificativa<textarea formControlName="note" maxlength="500" rows="3" required></textarea></label><div class="pagination"><button [disabled]="rejection.invalid || busy()">Confirmar rejeição</button><button type="button" class="secondary" (click)="rejecting.set(null)" [disabled]="busy()">Voltar</button></div></form></section> }
<section class="card"><div class="section-header"><h2>Registros</h2><button class="secondary" (click)="load(data().page)" [disabled]="loading() || busy()">Atualizar</button></div>@if (loading()) { <p role="status">Carregando perdas…</p> }<div class="table-scroll"><table><thead><tr><th>Data / produto</th><th>Quantidade</th><th>Motivo</th><th>Situação</th>@if (api.can('inventory:cost')) { <th>Custo (R$)</th> }<th>Ações</th></tr></thead><tbody>@for (loss of data().items; track loss.id) { <tr><td>{{loss.productName}}<small class="code-list">{{loss.createdAt | date:'dd/MM/yyyy HH:mm'}}</small></td><td>{{decimal(loss.quantity)}} {{loss.unit}}</td><td>{{reason(loss.reason)}}<small class="code-list">{{loss.details}} {{loss.reviewNote}}</small></td><td><span class="badge">{{status(loss.status)}}</span></td>@if (api.can('inventory:cost')) { <td>{{decimal(loss.approvedTotal ?? loss.estimatedTotal)}}<small class="code-list">{{loss.status === 'APPROVED' ? 'Efetivo' : 'Estimado'}}</small></td> }<td>@if (loss.status === 'PENDING' && api.can('loss:approve')) { <div class="row-actions"><button (click)="approve(loss)" [disabled]="busy()">Aprovar e baixar</button><button class="secondary" (click)="rejecting.set(loss); rejection.reset()" [disabled]="busy()">Rejeitar</button></div> }</td></tr> } @empty { <tr><td colspan="6">Nenhuma perda registrada. Utilize a tela Estoque para registrar uma perda.</td></tr> }</tbody></table></div><div class="pagination"><span>{{data().total}} perdas · página {{data().page+1}}</span><button class="secondary" (click)="load(data().page-1)" [disabled]="data().page===0 || loading() || busy()">Anterior</button><button class="secondary" (click)="load(data().page+1)" [disabled]="(data().page+1)*20>=data().total || loading() || busy()">Próxima</button></div></section>`})
export class Losses {
  readonly api = inject(Api); private fb = inject(FormBuilder);
  readonly data = signal<Page<StockLoss>>({items: [], total: 0, page: 0, size: 20}); readonly error = signal(''); readonly message = signal('');
  readonly loading = signal(false); readonly busy = signal(false); readonly rejecting = signal<StockLoss | null>(null);
  readonly rejection = this.fb.nonNullable.group({note: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(500)]]});
  private readonly approvalIds = new Map<string, string>();
  constructor() { this.load(0); }
  decimal(value: string | null) { return value === null ? 'Restrito' : value.replace('.', ','); }
  status(value: string) { return ({PENDING: 'Pendente', APPROVED: 'Aprovada', REJECTED: 'Rejeitada'} as Record<string, string>)[value] ?? value; }
  reason(value: string) { return ({EXPIRY: 'Vencimento', DAMAGE: 'Avaria', BREAKAGE: 'Quebra', THEFT: 'Furto', MISPLACEMENT: 'Extravio', DETERIORATION: 'Deterioração', OPERATIONAL_ERROR: 'Erro operacional', TRANSPORT: 'Transporte', STORAGE: 'Armazenamento', TECHNICAL: 'Quebra técnica', OTHER: 'Outro'} as Record<string, string>)[value] ?? value; }
  load(page: number) { this.loading.set(true); this.error.set(''); this.api.losses(page).pipe(finalize(() => this.loading.set(false))).subscribe({next: data => this.data.set(data), error: e => this.error.set(errorMessage(e))}); }
  approve(loss: StockLoss) {
    if (this.busy()) return;
    this.busy.set(true); this.error.set(''); this.message.set('');
    const id = this.approvalIds.get(loss.id) ?? crypto.randomUUID(); this.approvalIds.set(loss.id, id);
    this.api.approveLoss(loss.id, id).pipe(finalize(() => this.busy.set(false))).subscribe({next: () => {this.approvalIds.delete(loss.id); this.message.set('Perda aprovada e estoque atualizado.'); this.load(this.data().page);}, error: e => this.error.set(errorMessage(e))});
  }
  reject() {
    const loss = this.rejecting(); if (!loss || this.rejection.invalid || this.busy()) return;
    this.busy.set(true); this.error.set(''); this.message.set('');
    this.api.rejectLoss(loss.id, this.rejection.controls.note.value).pipe(finalize(() => this.busy.set(false))).subscribe({next: () => {this.rejecting.set(null); this.message.set('Perda rejeitada, sem baixa de estoque.'); this.load(this.data().page);}, error: e => this.error.set(errorMessage(e))});
  }
}
