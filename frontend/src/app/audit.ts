import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { finalize } from 'rxjs';
import { Api, AuditEvent, Page, errorMessage } from './api';

@Component({ standalone: true, imports: [DatePipe], template: `
  <p class="eyebrow">RASTREABILIDADE</p><h1>Auditoria</h1><p>Histórico de acessos e operações da empresa.</p>
  @if (error()) { <p class="error" role="alert">{{error()}}</p> }
  <section class="card"><div class="section-header"><h2>Eventos registrados</h2><button class="secondary" (click)="load(page())" [disabled]="loading()">Atualizar</button></div>
  @if (loading()) { <p role="status">Carregando eventos…</p> } @else { <div class="table-scroll"><table><thead><tr><th>Data e hora</th><th>Operação</th><th>Registro</th></tr></thead><tbody>@for (event of data().items; track event.id) { <tr><td>{{event.occurredAt | date:'dd/MM/yyyy HH:mm:ss'}}</td><td>{{event.action}}</td><td class="mono">{{event.entityId}}</td></tr> } @empty { <tr><td colspan="3">Nenhum evento registrado.</td></tr> }</tbody></table></div> }
  <div class="pagination"><span>{{data().total}} eventos · Página {{page()+1}}</span><button class="secondary" (click)="load(page()-1)" [disabled]="page()===0 || loading()">Anterior</button><button class="secondary" (click)="load(page()+1)" [disabled]="(page()+1)*20>=data().total || loading()">Próxima</button></div></section>` })
export class Audit {
  private api = inject(Api);
  readonly data = signal<Page<AuditEvent>>({items: [], total: 0, page: 0, size: 20});
  readonly error = signal(''); readonly loading = signal(false); readonly page = signal(0);
  constructor() { this.load(0); }
  load(page: number) {
    this.loading.set(true); this.error.set('');
    this.api.audit(page).pipe(finalize(() => this.loading.set(false))).subscribe({ next: data => { this.data.set(data); this.page.set(page); }, error: e => this.error.set(errorMessage(e)) });
  }
}
