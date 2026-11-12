import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export interface Session { token: string; expiresAt: string; tenantId: string; name: string; permissions: string[]; allBranches: boolean }
export interface PosTerminal { id: string; branchId: string; accountId: string; warehouseId: string | null; code: string; name: string; active: boolean; version: number }
export interface PosTerminalInput { requestId: string; branchId: string; accountId: string; code: string; name: string }
export interface PosCashOpening { requestId: string; terminalId: string; openingAmount: string; note: string }
export interface PosCashSession { id: string; terminalId: string; branchId: string; accountId: string; operatorId: string; operatorName: string; terminalCode: string; terminalName: string; status: string; version: number; openingAmount: string; openingNote: string; openedAt: string; expectedAmount: string; countedAmount: string | null; difference: string | null; closeNote: string | null; closedBy: string | null; closedAt: string | null }
export interface PosCashMoveInput { requestId: string; kind: string; otherAccountId: string | null; amount: string; note: string }
export interface PosCashMovement { id: string; sessionId: string; kind: string; otherAccountId: string | null; amount: string; note: string; treasuryOperationId: string; createdBy: string; createdAt: string }
export interface PosCashClosing { requestId: string; version: number; countedAmount: string; note: string }
export interface StatementInput { externalId: string; postedOn: string; amount: string; description: string }
export interface StatementLine extends StatementInput { id: string; accountId: string; status: string; version: number; matchedEntryId: string | null; reviewNote: string | null; reviewedBy: string | null; reviewedAt: string | null }
export interface StatementImport { id: string; accountId: string; reference: string; newCount: number; reusedCount: number; createdBy: string; createdAt: string }
export interface StatementCandidate { id: string; operationId: string; amount: string; note: string; origin: string; createdAt: string }
export interface StatementHistory { id: string; action: string; entryId: string | null; note: string; createdBy: string; createdAt: string; lineVersion: number }
export interface StatementCsvInput { requestId: string; accountId: string; reference: string; content: string }
export interface StatementBatchInput { requestId: string; accountId: string; reference: string; lines: StatementInput[] }
export interface StatementReview { requestId: string; version: number; entryId: string | null; note: string }
export interface CashFlowSummary { currentBalance: string; openingBalance: string; closingBalance: string; inflows: string; outflows: string; transferIn: string; transferOut: string; pendingReceivables: string; pendingPayables: string; overdueReceivables: string; overduePayables: string; unlinkedReceipts: string; unlinkedPayments: string }
export interface CashFlowReport { from: string; to: string; branchId: string | null; today: string; zone: string; generatedAt: string; summary: CashFlowSummary; actual: {date: string; inflows: string; outflows: string; transferIn: string; transferOut: string; closingBalance: string}[]; pending: {date: string; receivables: string; payables: string; net: string}[] }
export interface TreasuryInput { requestId: string; branchId: string; code: string; name: string; kind: string }
export interface TreasuryAccount { id: string; branchId: string; code: string; name: string; kind: string; balance: string }
export interface TreasuryMovement { requestId: string; sourceId: string | null; destinationId: string | null; amount: string; note: string }
export interface TreasuryEntry { id: string; operationId: string; delta: string; balanceAfter: string; note: string; createdBy: string; createdAt: string; origin: string }
export interface BranchScope { allBranches: boolean; branchIds: string[]; version: number }
export interface SupplierInput { code: string; name: string; taxIdentifier: string; contactName: string; email: string; phone: string; address: string; notes: string }
export interface Supplier extends SupplierInput { id: string; active: boolean; version: number }
export interface PurchaseHeader { id: string; branchId: string; supplierId: string; supplierName: string; notes: string; status: string; version: number; total: string; createdBy: string; createdAt: string; cancellationNote: string | null }
export interface PurchaseLine { id: string; productId: string; sku: string; name: string; unit: string; quantity: string; receivedQuantity: string; unitCost: string; total: string }
export interface PurchaseOrder { header: PurchaseHeader; lines: PurchaseLine[] }
export interface PurchaseInput { requestId: string; branchId: string; supplierId: string; notes: string; items: {productId: string; quantity: string; unitCost: string}[] }
export interface PurchaseReceiptInput { receiptId: string; warehouseId: string; note: string; items: {orderLineId: string; quantity: string}[] }
export interface PurchaseReceipt { id: string; orderId: string; warehouseId: string; note: string; createdBy: string; createdAt: string; lines: {orderLineId: string; operationId: string; quantity: string;amount:string}[] }
export interface PayableFilter {query?:string;dueFrom?:string;dueTo?:string;branchId?:string;supplierId?:string;receiptId?:string;withReceipt?:boolean}
export interface PayableTotals {originalAmount:string;paidAmount:string;outstandingAmount:string;overdueAmount:string}
export interface PayableInput { requestId: string; branchId: string; supplierId: string; purchaseReceiptId: string | null; reference: string; document: string; description: string; issuedOn: string; dueOn: string; amount: string }
export interface Payable extends Omit<PayableInput,'requestId'> { id: string; supplierName: string; paidAmount: string; creditedAmount:string; outstanding: string; status: string; version: number; createdBy: string; createdAt: string; cancellationNote: string | null; overdue: boolean }
export interface PayablePaymentInput { paymentId: string; amount: string; paidOn: string; method: string; reference: string; note: string; treasuryAccountId: string | null }
export interface PayablePayment { id: string; payableId: string; amount: string; paidOn: string; method: string; reference: string; note: string; status: string; createdBy: string; createdAt: string; reversalRequestId: string | null; reversalNote: string | null; reversedBy: string | null; reversedAt: string | null; treasuryAccountId: string | null; treasuryOperationId: string | null; treasuryReversalOperationId: string | null }
export interface CustomerInput { code: string; name: string; taxIdentifier: string; contactName: string; email: string; phone: string; address: string; notes: string }
export interface Customer extends CustomerInput { id: string; active: boolean; version: number }
export interface ReceivableInput { requestId: string; branchId: string; customerId: string; reference: string; document: string; description: string; issuedOn: string; dueOn: string; amount: string }
export interface ReceivablePlanInput extends Omit<ReceivableInput,'dueOn'> { firstDueOn: string; installmentCount: number; intervalMonths: number }
export interface ReceivablePlanPreview { amount: string; installments: {position: number; reference: string; dueOn: string; amount: string}[] }
export interface ReceivablePlan { id: string; branchId: string; customerId: string; reference: string; amount: string; installments: Receivable[] }
export interface Receivable extends Omit<ReceivableInput,'requestId'> { id: string; customerName: string; creditedAmount: string; receivedAmount: string; outstanding: string; status: string; version: number; createdBy: string; createdAt: string; cancellationNote: string | null; overdue: boolean }
export interface ReceivableFilter {query?:string;dueFrom?:string;dueTo?:string;branchId?:string;customerId?:string;planId?:string;saleId?:string;origin?:string}
export interface ReceivableTotals {originalAmount:string;receivedAmount:string;creditedAmount:string;outstandingAmount:string;overdueAmount:string}
export interface ReceivableOrigin {planId:string|null;position:number|null;installmentCount:number|null;saleId:string|null;reference:string|null}
export interface ReceivableAdjustment {returnId:string;saleId:string;kind:string;amount:string;createdAt:string;note:string;receiptId:string|null;treasuryOperationId:string|null}
export interface ReceivableReceiptInput { receiptId: string; amount: string; receivedOn: string; method: string; reference: string; note: string; treasuryAccountId: string | null }
export interface ReceivableReceipt { refundedAmount: string; id: string; receivableId: string; amount: string; receivedOn: string; method: string; reference: string; note: string; status: string; createdBy: string; createdAt: string; reversalRequestId: string | null; reversalNote: string | null; reversedBy: string | null; reversedAt: string | null; treasuryAccountId: string | null; treasuryOperationId: string | null; treasuryReversalOperationId: string | null }
export interface ActiveSession { createdAt: string; expiresAt: string; current: boolean }
export interface User { id: string; name: string; email: string; active: boolean; version: number; permissions: string[] }
export interface UserInput { name: string; email: string; password: string; permissions: string[] }
export interface AccessProfile { id: string; name: string; version: number; permissions: string[] }
export interface Branch { id: string; code: string; name: string; active: boolean; version: number }
export interface Page<T> { items: T[]; total: number; page: number; size: number }
export interface AuditEvent { id: string; actorId: string; action: string; entityId: string; occurredAt: string }
export interface Barcode { code: string; kind: 'GTIN' | 'INTERNAL' }
export interface Product { id: string; sku: string; name: string; description: string; unit: string; salePrice: string; active: boolean; version: number; barcodes: Barcode[] }
export interface ProductInput { sku: string; name: string; description: string; unit: string; salePrice: string; barcodes: Barcode[] }
export interface Warehouse { id: string; branchId: string; code: string; name: string; active: boolean }
export interface StockBalance { productId: string; sku: string; name: string; unit: string; quantity: string; averageCost: string | null }
export interface StockMovement { id: string; operationId: string; warehouseId: string; productId: string; type: string; quantity: string; balanceAfter: string; unit: string; unitCost: string | null; reason: string; actorId: string; createdAt: string }
export interface MovementInput { operationId: string; warehouseId: string; productId: string; type: string; quantity: string; unitCost: string | null; reason: string }
export interface TransferInput { operationId: string; sourceWarehouseId: string; destinationWarehouseId: string; productId: string; quantity: string; reason: string }
export interface LossInput { requestId: string; warehouseId: string; productId: string; quantity: string; reason: string; details: string }
export interface StockLoss { id: string; warehouseId: string; productId: string; productName: string; unit: string; quantity: string; reason: string; details: string; status: string; estimatedTotal: string | null; approvedTotal: string | null; reviewNote: string | null; createdAt: string }
@Injectable({ providedIn: 'root' })
export class Api {
  private http = inject(HttpClient);
  readonly session = signal<Session | null>(null);
  readonly inventoryPending = signal(false);
  login(credentials: { company: string; email: string; password: string }) { return this.http.post<Session>('/api/v1/auth/login', credentials); }
  posOperator() { return this.http.get<{userId: string}>('/api/v1/pos/operator'); }
  posTerminals(page: number) { return this.http.get<Page<PosTerminal>>('/api/v1/pos/terminals',{params:{page,size:20}}); }
  createPosTerminal(input: PosTerminalInput) { return this.http.post<PosTerminal>('/api/v1/pos/terminals',input); }
  setPosTerminalActive(id: string, input: {active: boolean; version: number}) { return this.http.put<PosTerminal>(`/api/v1/pos/terminals/${id}/active`,input); }
  posCashSessions(status: string, page: number) { return this.http.get<Page<PosCashSession>>('/api/v1/pos/cash-sessions',{params:{status,page,size:20}}); }
  posCashSession(id: string) { return this.http.get<PosCashSession>(`/api/v1/pos/cash-sessions/${id}`); }
  openPosCash(input: PosCashOpening) { return this.http.post<PosCashSession>('/api/v1/pos/cash-sessions',input); }
  posCashMovements(id: string, page: number) { return this.http.get<Page<PosCashMovement>>(`/api/v1/pos/cash-sessions/${id}/movements`,{params:{page,size:20}}); }
  movePosCash(id: string, input: PosCashMoveInput) { return this.http.post<PosCashMovement>(`/api/v1/pos/cash-sessions/${id}/movements`,input); }
  closePosCash(id: string, input: PosCashClosing) { return this.http.post<PosCashSession>(`/api/v1/pos/cash-sessions/${id}/close`,input); }
  statementCalendar() { return this.http.get<{date: string; zone: string}>('/api/v1/reconciliation/calendar'); }
  previewStatement(input: StatementCsvInput) { return this.http.post<{lines: StatementInput[]}>('/api/v1/reconciliation/imports/csv/preview', input); }
  importStatementCsv(input: StatementCsvInput) { return this.http.post<StatementImport>('/api/v1/reconciliation/imports/csv', input); }
  importStatement(input: StatementBatchInput) { return this.http.post<StatementImport>('/api/v1/reconciliation/imports', input); }
  statementImports(account: string, page: number) { return this.http.get<Page<StatementImport>>(`/api/v1/reconciliation/accounts/${account}/imports`, {params: {page,size:20}}); }
  statementSummary(account: string) { return this.http.get<{UNMATCHED: number; MATCHED: number; IGNORED: number}>(`/api/v1/reconciliation/accounts/${account}/summary`); }
  statementLines(account: string, status: string, importId: string, page: number) { return this.http.get<Page<StatementLine>>(`/api/v1/reconciliation/accounts/${account}/lines`, {params: importId?{status,importId,page,size:20}:{status,page,size:20}}); }
  statementLine(id: string) { return this.http.get<StatementLine>(`/api/v1/reconciliation/lines/${id}`); }
  statementCandidates(id: string, page: number) { return this.http.get<Page<StatementCandidate>>(`/api/v1/reconciliation/lines/${id}/candidates`, {params:{page,size:20}}); }
  statementHistory(id: string, page: number) { return this.http.get<Page<StatementHistory>>(`/api/v1/reconciliation/lines/${id}/history`, {params:{page,size:20}}); }
  reviewStatement(id: string, action: string, input: StatementReview) { return this.http.post<StatementLine>(`/api/v1/reconciliation/lines/${id}/${action}`, input); }
  cashFlowCalendar() { return this.http.get<{date: string; zone: string}>('/api/v1/cash-flow/calendar'); }
  cashFlow(from: string, to: string, branchId: string) { return this.http.get<CashFlowReport>('/api/v1/cash-flow', {params: branchId ? {from,to,branchId} : {from,to}}); }
  treasuryAccounts(page: number) { return this.http.get<Page<TreasuryAccount>>('/api/v1/treasury/accounts', {params: {page, size: 20}}); }
  treasuryAccount(id: string) { return this.http.get<TreasuryAccount>(`/api/v1/treasury/accounts/${id}`); }
  createTreasuryAccount(input: TreasuryInput) { return this.http.post<TreasuryAccount>('/api/v1/treasury/accounts', input); }
  treasuryMove(input: TreasuryMovement) { return this.http.post('/api/v1/treasury/movements', input); }
  treasuryEntries(id: string, page: number) { return this.http.get<Page<TreasuryEntry>>(`/api/v1/treasury/accounts/${id}/entries`, {params: {page, size: 20}}); }
  logout() { return this.http.post<void>('/api/v1/auth/logout', {}); }
  activeSessions() { return this.http.get<ActiveSession[]>('/api/v1/auth/sessions'); }
  revokeOtherSessions() { return this.http.delete<void>('/api/v1/auth/sessions/others'); }
  changePassword(input: {currentPassword: string; newPassword: string}) { return this.http.post<void>('/api/v1/auth/password', input); }
  users(page: number) { return this.http.get<Page<User>>('/api/v1/users', {params: {page, size: 20}}); }
  createUser(input: UserInput) { return this.http.post<User>('/api/v1/users', input); }
  setUserActive(user: User, active: boolean) { return this.http.put<User>(`/api/v1/users/${user.id}/active`, {active, version: user.version}); }
  userBranchScope(id: string) { return this.http.get<BranchScope>(`/api/v1/users/${id}/branches`); }
  setUserBranchScope(user: User, allBranches: boolean, branchIds: string[]) { return this.http.put<BranchScope>(`/api/v1/users/${user.id}/branches`, {allBranches, branchIds, version: user.version}); }
  setUserPermissions(user: User, permissions: string[]) { return this.http.put<User>(`/api/v1/users/${user.id}/permissions`, {permissions, version: user.version}); }
  me() { return this.http.get<Omit<Session, 'token' | 'expiresAt'>>('/api/v1/auth/me'); }
  suppliers(query: string, page: number) { return this.http.get<Page<Supplier>>('/api/v1/suppliers', {params: {q: query, page, size: 20}}); }
  supplier(id: string) { return this.http.get<Supplier>(`/api/v1/suppliers/${id}`); }
  createSupplier(data: SupplierInput) { return this.http.post<Supplier>('/api/v1/suppliers', data); }
  updateSupplier(supplier: Supplier, data: SupplierInput, active: boolean) { return this.http.put<Supplier>(`/api/v1/suppliers/${supplier.id}`, {data, active, version: supplier.version}); }
  purchaseOrders(page: number) { return this.http.get<Page<PurchaseHeader>>('/api/v1/purchase-orders', {params: {page, size: 20}}); }
  purchaseOrder(id: string) { return this.http.get<PurchaseOrder>(`/api/v1/purchase-orders/${id}`); }
  createPurchaseOrder(input: PurchaseInput) { return this.http.post<PurchaseOrder>('/api/v1/purchase-orders', input); }
  receivePurchase(id: string, input: PurchaseReceiptInput) { return this.http.post<PurchaseReceipt>(`/api/v1/purchase-orders/${id}/receipts`, input); }
  purchaseReceipts(id: string, page: number) { return this.http.get<Page<PurchaseReceipt>>(`/api/v1/purchase-orders/${id}/receipts`, {params: {page, size: 20}}); }
  cancelPurchase(id: string, input: {version: number; note: string}) { return this.http.post<PurchaseOrder>(`/api/v1/purchase-orders/${id}/cancel`, input); }
  payables(status: string, overdue: boolean, page: number,filter:PayableFilter={}) { return this.http.get<Page<Payable>&{summary:PayableTotals;today:string;zone:string}>('/api/v1/payables', {params: {...filter,status,overdue,page,size:20}}); }
  payable(id: string) { return this.http.get<Payable>(`/api/v1/payables/${id}`); }
  payableCalendar() { return this.http.get<{date: string; zone: string}>('/api/v1/payables/calendar'); }
  createPayable(input: PayableInput) { return this.http.post<Payable>('/api/v1/payables', input); }
  payPayable(id: string, input: PayablePaymentInput) { return this.http.post<PayablePayment>(`/api/v1/payables/${id}/payments`, input); }
  payablePayments(id: string, page: number) { return this.http.get<Page<PayablePayment>>(`/api/v1/payables/${id}/payments`, {params: {page, size: 20}}); }
  reversePayablePayment(id: string, paymentId: string, input: {requestId: string; note: string}) { return this.http.post<PayablePayment>(`/api/v1/payables/${id}/payments/${paymentId}/reverse`, input); }
  cancelPayable(id: string, input: {version: number; note: string}) { return this.http.post<Payable>(`/api/v1/payables/${id}/cancel`, input); }
  accessProfiles(page: number) { return this.http.get<Page<AccessProfile>>('/api/v1/access-profiles', {params: {page, size: 20}}); }
  createAccessProfile(input: {name: string; permissions: string[]}) { return this.http.post<AccessProfile>('/api/v1/access-profiles', input); }
  updateAccessProfile(profile: AccessProfile, input: {name: string; permissions: string[]}) { return this.http.put<AccessProfile>(`/api/v1/access-profiles/${profile.id}`, {...input, version: profile.version}); }
  applyAccessProfile(user: User, profile: AccessProfile) { return this.http.put<User>(`/api/v1/users/${user.id}/profile`, {profileId: profile.id, profileVersion: profile.version, userVersion: user.version}); }
  branches(page: number, size = 20) { return this.http.get<Page<Branch>>('/api/v1/branches', {params: {page, size}}); }
  branch(id: string) { return this.http.get<Branch>(`/api/v1/branches/${id}`); }
  createBranch(input: { code: string; name: string }) { return this.http.post<Branch>('/api/v1/branches', input); }
  audit(page: number) { return this.http.get<Page<AuditEvent>>(`/api/v1/audit?page=${page}&size=20`); }
  products(query: string, page: number, active?:boolean) { return this.http.get<Page<Product>>('/api/v1/products', {params: {q: query, page, size: 20,...(active===undefined?{}:{active})}}); }
  product(id:string){return this.http.get<Product>(`/api/v1/products/${id}`);}
  updateProduct(id:string,input:{version:number;product:ProductInput}){return this.http.put<Product>(`/api/v1/products/${id}`,input);}
  setProductActive(id:string,input:{version:number;active:boolean}){return this.http.put<Product>(`/api/v1/products/${id}/active`,input);}
  createProduct(input: ProductInput) { return this.http.post<Product>('/api/v1/products', input); }
  lookupProduct(code: string, kind: Barcode['kind']) { return this.http.get<Product>('/api/v1/products/lookup', {params: {code, kind}}); }
  warehouses(page: number, size = 20) { return this.http.get<Page<Warehouse>>('/api/v1/inventory/warehouses', {params: {page, size}}); }
  createWarehouse(input: {branchId: string; code: string; name: string}) { return this.http.post<Warehouse>('/api/v1/inventory/warehouses', input); }
  stockBalances(warehouseId: string, page: number) { return this.http.get<Page<StockBalance>>(`/api/v1/inventory/warehouses/${warehouseId}/balances`, {params: {page, size: 20}}); }
  stockMovements(warehouseId: string, page: number) { return this.http.get<Page<StockMovement>>(`/api/v1/inventory/warehouses/${warehouseId}/movements`, {params: {page, size: 20}}); }
  moveStock(input: MovementInput) { return this.http.post<{operationId: string; movements: StockMovement[]}>('/api/v1/inventory/movements', input); }
  transferStock(input: TransferInput) { return this.http.post<{operationId: string; movements: StockMovement[]}>('/api/v1/inventory/transfers', input); }
  reportLoss(input: LossInput) { return this.http.post<StockLoss>('/api/v1/losses', input); }
  losses(page: number) { return this.http.get<Page<StockLoss>>('/api/v1/losses', {params: {page, size: 20}}); }
  approveLoss(id: string, operationId: string) { return this.http.post<StockLoss>(`/api/v1/losses/${id}/approve`, {operationId}); }
  rejectLoss(id: string, note: string) { return this.http.post<StockLoss>(`/api/v1/losses/${id}/reject`, {note}); }
  customers(query: string, page: number) { return this.http.get<Page<Customer>>('/api/v1/customers', {params: {q: query, page, size: 20}}); }
  customer(id: string) { return this.http.get<Customer>(`/api/v1/customers/${id}`); }
  createCustomer(data: CustomerInput) { return this.http.post<Customer>('/api/v1/customers', data); }
  updateCustomer(customer: Customer, data: CustomerInput, active: boolean) { return this.http.put<Customer>(`/api/v1/customers/${customer.id}`, {data, active, version: customer.version}); }
  receivables(status: string, overdue: boolean, page: number,filter:ReceivableFilter={}) { return this.http.get<Page<Receivable>&{summary:ReceivableTotals;today:string;zone:string}>('/api/v1/receivables', {params: {...filter,status,overdue,page,size:20}}); }
  receivableOrigin(id:string){return this.http.get<ReceivableOrigin>(`/api/v1/receivables/${id}/origin`);}
  receivable(id: string) { return this.http.get<Receivable>(`/api/v1/receivables/${id}`); }
  receivableCalendar() { return this.http.get<{date: string; zone: string}>('/api/v1/receivables/calendar'); }
  createReceivable(input: ReceivableInput) { return this.http.post<Receivable>('/api/v1/receivables', input); }
  previewReceivablePlan(input: ReceivablePlanInput) { return this.http.post<ReceivablePlanPreview>('/api/v1/receivables/plans/preview', input); }
  createReceivablePlan(input: ReceivablePlanInput) { return this.http.post<ReceivablePlan>('/api/v1/receivables/plans', input); }
  receivableAdjustments(id:string,page:number){return this.http.get<Page<ReceivableAdjustment>>(`/api/v1/receivables/${id}/adjustments`,{params:{page,size:20}});}
  receiveReceivable(id: string, input: ReceivableReceiptInput) { return this.http.post<ReceivableReceipt>(`/api/v1/receivables/${id}/receipts`, input); }
  receivableReceipts(id: string, page: number) { return this.http.get<Page<ReceivableReceipt>>(`/api/v1/receivables/${id}/receipts`, {params: {page, size: 20}}); }
  reverseReceivableReceipt(id: string, receiptId: string, input: {requestId: string; note: string}) { return this.http.post<ReceivableReceipt>(`/api/v1/receivables/${id}/receipts/${receiptId}/reverse`, input); }
  cancelReceivable(id: string, input: {version: number; note: string}) { return this.http.post<Receivable>(`/api/v1/receivables/${id}/cancel`, input); }
  can(permission: string) { const session=this.session(); return !!session && session.permissions.includes(permission) && (session.allBranches || !['user:read','user:write','audit:read','branch:write'].includes(permission)) && (permission!=='cashflow:read' || ['treasury:read','payable:read','receivable:read'].every(required=>session.permissions.includes(required))) && (!['reconciliation:read','pos:read'].includes(permission) || session.permissions.includes('treasury:read')); }
}
export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  const api = inject(Api), router = inject(Router), token = api.session()?.token;
  const sameApi = request.url.startsWith('/api/');
  return next(sameApi && token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request).pipe(catchError((error: HttpErrorResponse) => {
    if (sameApi && error.status === 401 && !request.url.endsWith('/login')) { api.session.set(null); void router.navigateByUrl('/login'); }
    return throwError(() => error);
  }));
};
export function errorMessage(error: HttpErrorResponse): string {
  return error.status === 0 ? 'Servidor indisponível. Confira a conexão e tente novamente.' : error.error?.detail ?? 'Não foi possível concluir a operação.';
}
