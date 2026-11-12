export interface AllocationPayment {method:string;amount:string;tendered?:string;accountId?:string|null;reference?:string|null;confirmed?:boolean}
export interface AllocationSummary {applied:string;tendered:string;change:string;remaining:string;excess:string;balanced:boolean;errors:string[]}
const max=9999999999999999999n;
function cents(value:string|undefined,zero=false):bigint|null {
 if(value===undefined||!/^\d{1,17}(\.\d{1,2})?$/.test(value))return null;
 const [whole,fraction='']=value.split('.');const n=BigInt(whole)*100n+BigInt(fraction.padEnd(2,'0'));
 return n>max||(!zero&&n===0n)?null:n;
}
function format(n:bigint):string {return `${n/100n}.${(n%100n).toString().padStart(2,'0')}`;}
/** Informational only: no receipt, account lookup, reservation or bank confirmation. */
export function paymentAllocation(total:string,payments:readonly AllocationPayment[],legacyTendered?:string):AllocationSummary {
 const errors:string[]=[];const target=cents(total);let applied=0n,received=0n,change=0n,cash=0;
 if(target===null)errors.push('Total inválido.');
 if(payments.length>100)errors.push('Use no máximo cem pagamentos.');
 const inputs=legacyTendered===undefined?payments:[{method:'CASH',amount:total,tendered:legacyTendered}];
 if(legacyTendered!==undefined&&payments.length)errors.push('Use dinheiro recebido ou pagamentos divididos, sem combinar os dois.');
 const proofs=new Set<string>();
 for(const [index,p] of inputs.entries()){
  const label=`Pagamento ${index+1}: `;const amount=cents(p.amount),tendered=cents(p.tendered??p.amount);
  if(amount===null||tendered===null){errors.push(label+'informe valores positivos com até duas casas decimais.');continue;}
  const isCash=p.method==='CASH';const reference=p.reference?.trim()??'';
  if(!['CASH','PIX','BANK_TRANSFER'].includes(p.method))errors.push(label+'meio inválido.');
  if(isCash){if(++cash>1)errors.push('Use apenas uma parcela em dinheiro.');if(reference)errors.push(label+'dinheiro não usa comprovante externo.');}
  else {
   if(!p.accountId||p.confirmed!==true||!reference)errors.push(label+'selecione conta, comprovante e confirme o recebimento.');
   if(reference.length>160)errors.push(label+'comprovante excede 160 caracteres.');
   const key=JSON.stringify([p.accountId,p.method,reference]);if(proofs.has(key))errors.push(label+'comprovante repetido nesta conta e meio.');proofs.add(key);
  }
  if(tendered<amount)errors.push(label+'recebido menor que o valor aplicado.');
  if(!isCash&&tendered!==amount)errors.push(label+'troco é permitido somente em dinheiro.');
  applied+=amount;received+=tendered;if(isCash&&tendered>amount)change+=tendered-amount;
 }
 if(received>max)errors.push('Recebimento excede o limite monetário.');
 const delta=(target??0n)-applied;const remaining=delta>0n?delta:0n,excess=delta<0n?-delta:0n;
 return {applied:format(applied),tendered:format(received),change:format(change),remaining:format(remaining),excess:format(excess),balanced:errors.length===0&&remaining===0n&&excess===0n,errors};
}

function validEditIndex(payments:readonly AllocationPayment[],index:number|null):boolean {
 return index===null||(Number.isInteger(index)&&index>=0&&index<payments.length);
}
/** Excludes the edited line so that replacing it does not count its amount twice. */
export function suggestPaymentAmount(total:string,payments:readonly AllocationPayment[],index:number|null=null):string|null {
 if(!validEditIndex(payments,index))return null;
 const target=cents(total);if(target===null||payments.length>100)return null;let applied=0n;
 for(const [i,p] of payments.entries()){if(i===index)continue;const amount=cents(p.amount);if(amount===null)return null;applied+=amount;}
 return target>applied?format(target-applied):null;
}
/** Validates a proposed local edit; never mutates the list or registers a receipt. */
export function composePayment<T extends AllocationPayment>(total:string,payments:readonly T[],candidate:T,index:number|null=null):{payments:T[]|null;errors:string[]} {
 if(!validEditIndex(payments,index))return {payments:null,errors:['Parcela não encontrada. Cancele a edição e selecione novamente.']};
 const next=payments.map(p=>({...p}));const line={...candidate,reference:candidate.reference?.trim()??''};
 if(index===null)next.push(line);else next[index]=line;
 const summary=paymentAllocation(total,next);
 // Other rows may still need reconfirmation after switching sessions. Fix them one at a time.
 const previousErrors=new Set(paymentAllocation(total,payments).errors);
 const errors=[...paymentAllocation(total,[line]).errors,...summary.errors.filter(e=>!previousErrors.has(e))];
 let applied=0n;for(const p of next){const amount=cents(p.amount);if(amount!==null)applied+=amount;}
 const target=cents(total);
 if(target!==null&&applied>target)errors.push('O valor aplicado excede o total a receber. Reduza a parcela; excesso aplicado não é troco.');
 if(next.length>100&&!errors.includes('Use no máximo cem pagamentos.'))errors.push('Use no máximo cem pagamentos.');
 return {payments:errors.length?null:next,errors};
}
