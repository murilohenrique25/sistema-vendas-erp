export interface CartInputItem {productId:string;quantity:string;expectedUnitPrice:string;discountAmount:string}
export interface CartLine {productId:string;sku:string;name:string;unit:string;quantity:string;unitPrice:string;grossTotal:string;discountAmount:string;total:string}
export interface Suspension {createdByName:string;id:string;branchId:string;originalSessionId:string;lastSessionId:string;createdBy:string;status:string;version:number;note:string;grossTotal:string;discountTotal:string;total:string;items:CartLine[];updatedAt:string;saleId:string|null;discardNote:string|null}
export interface Approval {requestedByName:string;id:string;branchId:string;requestedBy:string;status:string;version:number;policyVersion:number;note:string;grossTotal:string;discountTotal:string;items:CartLine[];createdAt:string;decidedBy:string|null;decisionNote:string|null;expiresAt:string|null;saleId:string|null}
export interface DiscountPolicy {branchId:string;operatorPercent:string;version:number}
