import { Component, inject, signal } from '@angular/core';

import { bootstrapApplication } from '@angular/platform-browser';

import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { CanActivateFn, CanDeactivateFn, Router, RouterLink, RouterLinkActive, RouterOutlet, provideRouter } from '@angular/router';

import { Api, sessionInterceptor, errorMessage } from './app/api';

import { Login } from './app/login';

import { Branches } from './app/branches';

import { Audit } from './app/audit';

import { Products } from './app/products';

import { Inventory } from './app/inventory';

import { Losses } from './app/losses';

import { Account } from './app/account';

import { Users } from './app/users';

import { AccessProfiles } from './app/access-profiles';

import { Suppliers } from './app/suppliers';

import { Purchases } from './app/purchases';

import type { Payables } from './app/payables';

import { Treasury } from './app/treasury';

import type { Receivables } from './app/receivables';

import { Customers } from './app/customers';

import { CashFlow } from './app/cash-flow';



const guard: CanActivateFn = route => {

  const api = inject(Api), router = inject(Router);

  return api.session() && api.can(route.data['permission']) ? true : router.createUrlTree([api.session() ? '/account' : '/login']);

};

const posCashExit: CanDeactivateFn<{canLeave():boolean}> = component => component.canLeave();

const reconciliationExit: CanDeactivateFn<{canLeave():boolean}> = component => component.canLeave();

const receivablesExit: CanDeactivateFn<Receivables> = component => component.canLeave();

const treasuryExit: CanDeactivateFn<Treasury> = component => component.canLeave();

const payablesExit: CanDeactivateFn<Payables> = component => component.canLeave();

const purchasesExit: CanDeactivateFn<Purchases> = component => component.canLeave();

const inventoryExit: CanDeactivateFn<Inventory> = component => component.canLeave();

const accountGuard: CanActivateFn = () => inject(Api).session() ? true : inject(Router).createUrlTree(['/login']);

@Component({ selector: 'erp-root', standalone: true, imports: [RouterOutlet, RouterLink, RouterLinkActive], template: `

  @if (api.session()) { <div class="shell"><aside><div class="brand">ERP<span>COMERCIAL</span></div><p class="nav-label">ADMINISTRAÇÃO</p><nav aria-label="Navegação principal">@if(api.can('fiscal:read')){<a routerLink="/fiscal" routerLinkActive="selected">Fiscal · SEFAZ</a>}@if(api.can('commercial:read')){<a routerLink="/commercial-rules" routerLinkActive="selected">Regras comerciais</a>}@if(api.can('salesorder:read')){<a routerLink="/sales-orders" routerLinkActive="selected">Pedidos e orçamentos</a>}@if (api.can('pos:read')) { <a routerLink="/pos-cash" routerLinkActive="selected">PDV e caixa</a><a routerLink="/pos-sales" routerLinkActive="selected">Vendas</a> }@if (api.can('reconciliation:read')) { <a routerLink="/reconciliation" routerLinkActive="selected">Conciliação</a> }@if (api.can('cashflow:read')) { <a routerLink="/cash-flow" routerLinkActive="selected">Fluxo de caixa</a> }@if (api.can('customer:read')) { <a routerLink="/customers" routerLinkActive="selected">Clientes</a> }@if (api.can('customer:credit') && api.can('customer:read')) { <a routerLink="/customer-credit" routerLinkActive="selected">Crédito de clientes</a> }@if (api.can('receivable:read')) { <a routerLink="/receivables" routerLinkActive="selected">Contas a receber</a> }@if (api.can('treasury:read')) { <a routerLink="/treasury" routerLinkActive="selected">Tesouraria</a> }@if (api.can('payable:read')) { <a routerLink="/payables" routerLinkActive="selected">Contas a pagar</a> }@if (api.can('purchase:read')) { <a routerLink="/purchases" routerLinkActive="selected">Compras</a>@if(api.can('payable:read')&&api.can('inventory:read')){<a routerLink="/supplier-returns" routerLinkActive="selected">Devolução ao fornecedor</a>} }@if (api.can('supplier:read')) { <a routerLink="/suppliers" routerLinkActive="selected">Fornecedores</a> }@if (api.can('user:read')) { <a routerLink="/profiles" routerLinkActive="selected">Perfis de acesso</a> }@if (api.can('user:read')) { <a routerLink="/users" routerLinkActive="selected">Usuários</a> }<a routerLink="/account" routerLinkActive="selected">Segurança da conta</a>@if (api.can('branch:read')) { <a routerLink="/branches" routerLinkActive="selected">Filiais</a> } @if (api.can('product:read')) { <a routerLink="/products" routerLinkActive="selected">Produtos</a><a routerLink="/branch-prices" routerLinkActive="selected">Preços por filial</a> } @if (api.can('inventory:read')) { <a routerLink="/inventory" routerLinkActive="selected">Estoque</a><a routerLink="/replenishment" routerLinkActive="selected">Reposição</a><a routerLink="/stock-lots" routerLinkActive="selected">Lotes e validade</a><a routerLink="/inventory-counts" routerLinkActive="selected">Inventário físico</a> } @if (api.can('loss:read')) { <a routerLink="/losses" routerLinkActive="selected">Perdas</a> } @if (api.can('audit:read')) { <a routerLink="/audit" routerLinkActive="selected">Auditoria</a> }</nav><div class="sidebar-footer">Plataforma de gestão comercial</div></aside><div class="workspace"><header><span>{{api.session()?.name}}</span><div><button class="secondary" (click)="toggleTheme()">{{dark() ? 'Tema claro' : 'Tema escuro'}}</button><button class="secondary" (click)="logout()" [disabled]="api.inventoryPending()">Sair</button></div></header>@if (error()) { <p class="error" role="alert">{{error()}}</p> }<main id="main"><router-outlet /></main></div></div> } @else { <router-outlet /> }` })

class App {

  readonly api = inject(Api); private router = inject(Router); readonly dark = signal(false); readonly error = signal('');

  toggleTheme() { this.dark.update(value => !value); document.documentElement.dataset['theme'] = this.dark() ? 'dark' : 'light'; }

  logout() { this.api.logout().subscribe({ next: () => { this.api.session.set(null); void this.router.navigateByUrl('/login'); }, error: e => this.error.set(errorMessage(e)) }); }

}

bootstrapApplication(App, { providers: [provideHttpClient(withInterceptors([sessionInterceptor])), provideRouter([{path:'fiscal',loadComponent:()=>import('./app/fiscal').then(m=>m.Fiscal),canActivate:[guard],canDeactivate:[inventoryExit],data:{permission:'fiscal:read'}},{path:'commercial-rules',loadComponent:()=>import('./app/commercial-rules').then(m=>m.CommercialRules),canActivate:[guard],canDeactivate:[inventoryExit],data:{permission:'commercial:read'}},{path:'sales-orders',loadComponent:()=>import('./app/sales-orders').then(m=>m.SalesOrders),canActivate:[guard],canDeactivate:[inventoryExit],data:{permission:'salesorder:read'}},{path:'branch-prices',loadComponent:()=>import('./app/branch-prices').then(m=>m.BranchPrices),canActivate:[guard],canDeactivate:[inventoryExit],data:{permission:'product:read'}},{path:'supplier-returns',loadComponent:()=>import('./app/supplier-returns').then(m=>m.SupplierReturns),canActivate:[guard],canDeactivate:[inventoryExit],data:{permission:'purchase:read'}},{path:'replenishment',loadComponent:()=>import('./app/replenishment').then(m=>m.Replenishment),canActivate:[guard],canDeactivate:[inventoryExit],data:{permission:'inventory:read'}},{path:'stock-lots',loadComponent:()=>import('./app/lots').then(m=>m.Lots),canActivate:[guard],canDeactivate:[inventoryExit],data:{permission:'inventory:read'}},{path:'inventory-counts',loadComponent:()=>import('./app/physical-inventory').then(m=>m.PhysicalInventory),canActivate:[guard],canDeactivate:[inventoryExit],data:{permission:'inventory:read'}},{path: 'customer-credit', loadComponent: () => import('./app/customer-credit').then(m => m.CustomerCredit), canActivate: [guard], canDeactivate: [posCashExit], data: {permission: 'customer:credit'}}, {path: 'login', component: Login}, {path: 'pos-sales', loadComponent: () => import('./app/pos-sales').then(m => m.PosSales), canActivate: [guard], canDeactivate: [posCashExit], data: {permission: 'pos:read'}}, {path: 'pos-cash', loadComponent: () => import('./app/pos-cash').then(m => m.PosCash), canActivate: [guard], canDeactivate: [posCashExit], data: {permission: 'pos:read'}}, {path: 'reconciliation', loadComponent: () => import('./app/reconciliation').then(m => m.Reconciliation), canActivate: [guard], canDeactivate: [reconciliationExit], data: {permission: 'reconciliation:read'}}, {path: 'cash-flow', component: CashFlow, canActivate: [guard], data: {permission: 'cashflow:read'}}, {path: 'customers', component: Customers, canActivate: [guard], data: {permission: 'customer:read'}}, {path: 'receivables', loadComponent: () => import('./app/receivables').then(m => m.Receivables), canActivate: [guard], canDeactivate: [receivablesExit], data: {permission: 'receivable:read'}}, {path: 'treasury', component: Treasury, canActivate: [guard], canDeactivate: [treasuryExit], data: {permission: 'treasury:read'}}, {path: 'payables', loadComponent: () => import('./app/payables').then(m => m.Payables), canActivate: [guard], canDeactivate: [payablesExit], data: {permission: 'payable:read'}}, {path: 'purchases', component: Purchases, canActivate: [guard], canDeactivate: [purchasesExit], data: {permission: 'purchase:read'}}, {path: 'suppliers', component: Suppliers, canActivate: [guard], data: {permission: 'supplier:read'}}, {path: 'profiles', component: AccessProfiles, canActivate: [guard], data: {permission: 'user:read'}}, {path: 'users', component: Users, canActivate: [guard], data: {permission: 'user:read'}}, {path: 'account', component: Account, canActivate: [accountGuard]}, {path: 'branches', component: Branches, canActivate: [guard], data: {permission: 'branch:read'}}, {path: 'products', component: Products, canActivate: [guard], canDeactivate: [posCashExit], data: {permission: 'product:read'}}, {path: 'inventory', component: Inventory, canActivate: [guard], canDeactivate: [inventoryExit], data: {permission: 'inventory:read'}}, {path: 'losses', component: Losses, canActivate: [guard], data: {permission: 'loss:read'}}, {path: 'audit', component: Audit, canActivate: [guard], data: {permission: 'audit:read'}}, {path: '**', redirectTo: 'branches'}])] }).catch(console.error);

















