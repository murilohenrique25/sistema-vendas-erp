import { Component, HostListener, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, forkJoin, Observable } from 'rxjs';
import { Api, Branch, LossInput, MovementInput, Page, Product, StockBalance, StockMovement, TransferInput, Warehouse, errorMessage } from './api';

const emptyPage = <T>(): Page<T> => ({items: [], total: 0, page: 0, size: 20});
@Component({standalone: true, imports: [ReactiveFormsModule, DatePipe], templateUrl: './inventory.html'})
export class Inventory {
  readonly api = inject(Api); private fb = inject(FormBuilder);
  readonly branches = signal<Branch[]>([]); readonly warehouses = signal<Page<Warehouse>>(emptyPage()); readonly products = signal<Page<Product>>(emptyPage());
  readonly balances = signal<Page<StockBalance>>(emptyPage()); readonly movements = signal<Page<StockMovement>>(emptyPage());
  readonly busy = signal(false); readonly loading = signal(false); readonly catalogLoading = signal(false); readonly error = signal(''); readonly message = signal('');
  readonly pending = signal<MovementInput | TransferInput | LossInput | null>(null); readonly pendingKind = signal<'movement' | 'transfer' | 'loss' | null>(null);
  readonly branchPage = signal(0); readonly branchTotal = signal(0);
  private refreshSequence = 0;
  private displayedWarehouse = '';
  readonly warehouseForm = this.fb.nonNullable.group({branchId: ['', Validators.required], code: ['', [Validators.required, Validators.pattern('[A-Za-z0-9_-]{1,30}')]], name: ['', [Validators.required, Validators.maxLength(160)]]});
  readonly context = this.fb.nonNullable.group({warehouseId: ['', Validators.required], productId: ['', Validators.required]});
  readonly search = this.fb.nonNullable.group({q: ['', Validators.maxLength(200)]});
  readonly movementForm = this.fb.nonNullable.group({type: ['RECEIPT'], quantity: ['', [Validators.required, Validators.pattern('[0-9]{1,13}(\\.[0-9]{1,6})?')]], unitCost: ['', Validators.pattern('[0-9]{1,13}(\\.[0-9]{1,6})?')], reason: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(500)]]});
  readonly transferForm = this.fb.nonNullable.group({destinationWarehouseId: ['', Validators.required], quantity: ['', [Validators.required, Validators.pattern('[0-9]{1,13}(\\.[0-9]{1,6})?')]], reason: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(500)]]});
  readonly lossForm = this.fb.nonNullable.group({quantity: ['', [Validators.required, Validators.pattern('[0-9]{1,13}(\\.[0-9]{1,6})?')]], reason: ['DAMAGE'], details: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(500)]]});
  constructor() { this.loadWarehouses(0); if (this.api.can('branch:read')) this.loadBranches(0); }
  @HostListener('window:beforeunload', ['$event']) preventUncertainReload(event: BeforeUnloadEvent) { if (this.pending()) {event.preventDefault(); event.returnValue = ''; } }
  canLeave() { if (!this.pending()) return true; this.error.set('Conclua ou repita a operação pendente antes de sair desta tela.'); return false; }
  incoming() { return ['RECEIPT', 'ADJUSTMENT_IN'].includes(this.movementForm.controls.type.value); }
  decimal(value: string | null) { return value === null ? 'Restrito' : value.replace('.', ','); }
  typeLabel(value: string) { return ({RECEIPT: 'Entrada', ADJUSTMENT_IN: 'Ajuste de entrada', ADJUSTMENT_OUT: 'Ajuste de saída', INTERNAL_CONSUMPTION: 'Consumo interno', TRANSFER_IN: 'Transferência recebida', TRANSFER_OUT: 'Transferência enviada', LOSS: 'Perda aprovada'} as Record<string, string>)[value] ?? value; }
  loadBranches(page: number) {
    this.api.branches(page).subscribe({next: data => {this.branches.set(data.items); this.branchPage.set(page); this.branchTotal.set(data.total); this.warehouseForm.controls.branchId.setValue('');}, error: e => this.error.set(errorMessage(e))});
  }
  loadWarehouses(page: number) {
    if (this.pending() || this.busy()) return;
    this.catalogLoading.set(true);
    this.api.warehouses(page).pipe(finalize(() => this.catalogLoading.set(false))).subscribe({next: data => {this.warehouses.set(data); this.context.controls.warehouseId.setValue(''); this.transferForm.controls.destinationWarehouseId.setValue(''); this.balances.set(emptyPage()); this.movements.set(emptyPage());}, error: e => this.error.set(errorMessage(e))});
  }
  createWarehouse() {
    if (this.warehouseForm.invalid || this.busy() || this.pending()) return;
    this.busy.set(true); this.error.set('');
    this.api.createWarehouse(this.warehouseForm.getRawValue()).pipe(finalize(() => this.busy.set(false))).subscribe({next: () => {this.message.set('Depósito cadastrado.'); this.warehouseForm.reset(); this.api.warehouses(0).subscribe({next: data => this.warehouses.set(data), error: e => this.error.set(errorMessage(e))});}, error: e => this.error.set(errorMessage(e))});
  }
  searchProducts(page = 0) {
    if (this.search.invalid || this.pending() || this.busy()) return;
    this.catalogLoading.set(true); this.context.controls.productId.setValue('');
    this.api.products(this.search.controls.q.value, page).pipe(finalize(() => this.catalogLoading.set(false))).subscribe({next: data => this.products.set(data), error: e => this.error.set(errorMessage(e))});
  }
  refreshBalances(page = 0) { this.refresh(page, this.movements().page); }
  refreshMovements(page = 0) { this.refresh(this.balances().page, page); }
  refresh(balancePage = 0, movementPage = 0) {
    const warehouse = this.context.controls.warehouseId.value;
    if (!warehouse) return;
    const sequence = ++this.refreshSequence;
    if (warehouse !== this.displayedWarehouse) {this.balances.set(emptyPage()); this.movements.set(emptyPage());}
    this.displayedWarehouse = warehouse;
    this.loading.set(true); this.error.set('');
    forkJoin({balances: this.api.stockBalances(warehouse, balancePage), movements: this.api.stockMovements(warehouse, movementPage)}).pipe(finalize(() => {if (sequence === this.refreshSequence) this.loading.set(false);})).subscribe({next: data => {if (sequence === this.refreshSequence && warehouse === this.context.controls.warehouseId.value) {this.balances.set(data.balances); this.movements.set(data.movements);}}, error: e => {if (sequence === this.refreshSequence) this.error.set(errorMessage(e));}});
  }
  submitMovement() {
    if (this.context.invalid || this.movementForm.invalid || this.busy() || this.pending()) return;
    const input = this.movementForm.getRawValue();
    if (this.incoming() && !input.unitCost) { this.error.set('Informe o custo unitário da entrada.'); return; }
    this.pending.set({...input, ...this.context.getRawValue(), unitCost: this.incoming() ? input.unitCost : null, operationId: crypto.randomUUID()}); this.pendingKind.set('movement'); this.sendPending();
  }
  submitTransfer() {
    if (this.context.invalid || this.transferForm.invalid || this.busy() || this.pending()) return;
    this.pending.set({...this.transferForm.getRawValue(), sourceWarehouseId: this.context.controls.warehouseId.value, productId: this.context.controls.productId.value, operationId: crypto.randomUUID()}); this.pendingKind.set('transfer'); this.sendPending();
  }
  submitLoss() {
    if (this.context.invalid || this.lossForm.invalid || this.busy() || this.pending()) return;
    this.pending.set({...this.lossForm.getRawValue(), ...this.context.getRawValue(), requestId: crypto.randomUUID()}); this.pendingKind.set('loss'); this.sendPending();
  }
  sendPending() {
    const input = this.pending(), kind = this.pendingKind();
    if (!input || !kind || this.busy()) return;
    this.busy.set(true); this.error.set(''); this.message.set('');
    this.api.inventoryPending.set(true);
    this.context.disable(); this.movementForm.disable(); this.transferForm.disable(); this.lossForm.disable();
    const request: Observable<unknown> = kind === 'movement' ? this.api.moveStock(input as MovementInput) : kind === 'transfer' ? this.api.transferStock(input as TransferInput) : this.api.reportLoss(input as LossInput);
    request.pipe(finalize(() => this.busy.set(false))).subscribe({next: () => {this.clearPending(); if (kind === 'movement') this.movementForm.reset({type: 'RECEIPT'}); else if (kind === 'transfer') this.transferForm.reset(); else this.lossForm.reset({reason: 'DAMAGE'}); this.message.set(kind === 'loss' ? 'Perda registrada e aguardando aprovação. O saldo ainda não foi alterado.' : 'Operação registrada no histórico.'); this.refresh();}, error: (e: HttpErrorResponse) => {
      if (e.status >= 400 && e.status < 500) this.clearPending();
      this.error.set(errorMessage(e) + (this.pending() ? ' O resultado é incerto. Repita a mesma operação pelo botão abaixo; mantenha esta página aberta.' : ''));
    }});
  }
  private clearPending() { this.pending.set(null); this.pendingKind.set(null); this.api.inventoryPending.set(false); this.context.enable(); this.movementForm.enable(); this.transferForm.enable(); this.lossForm.enable(); }
}
