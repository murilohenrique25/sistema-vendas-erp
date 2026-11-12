export interface ReviewSnapshot<T> {readonly key:string;readonly data:T}
function freeze(value:unknown):void {
 if(value!==null&&typeof value==='object'){for(const child of Object.values(value))freeze(child);Object.freeze(value);}
}
/** Detached immutable JSON snapshot; not a payment authorization or server-side guarantee. */
export function createSaleReview<T>(data:T):ReviewSnapshot<T> {
 const key=JSON.stringify(data);const copy=JSON.parse(key) as T;freeze(copy);
 return Object.freeze({key,data:copy});
}
export function saleReviewMatches<T>(review:ReviewSnapshot<T>,current:T):boolean {
 return review.key===JSON.stringify(current);
}
