import type { AllocationPayment } from './payment-allocation';

/** Proposed amounts are retained; a drawer change never carries a receipt confirmation forward. */
export function paymentsForNewSession<T extends AllocationPayment>(payments:readonly T[],branchChanged:boolean):T[] {
 return payments.map(p=>({...p,confirmed:false,
  ...(p.method==='CASH'?{tendered:''}:branchChanged?{accountId:null}:{})
 }));
}

export interface SaleSessionContext {id:string;branchId:string;operatorId:string;status:string}
export function canUseSaleSession(session:SaleSessionContext,operatorId:string,supervisor:boolean,draftBranch:string|null,checkoutSession:string|null):boolean {
 return session.status==='OPEN'&&(session.operatorId===operatorId||supervisor)
  &&(draftBranch===null||session.branchId===draftBranch)
  &&(checkoutSession===null||session.id===checkoutSession);
}
