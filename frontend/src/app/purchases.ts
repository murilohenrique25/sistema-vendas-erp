import { RouterLink } from '@angular/router';
import { Component, HostListener, ViewChild, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, forkJoin, Observable } from 'rxjs';
import { PurchaseReceiptFinance } from './purchase-receipt-finance';
import { Api, Branch, Page, Product, PurchaseHeader, PurchaseInput, PurchaseOrder, PurchaseReceipt, PurchaseReceiptInput, Supplier, Warehouse, errorMessage } from './api';

type Pending = {kind:'create';input:PurchaseInput} | {kind:'receive';id:string;input:PurchaseReceiptInput} | {kind:'cancel';id:string;input:{version:number;note:string}};
const empty=<T>():Page<T>=>({items:[],total:0,page:0,size:20});
@Component({standalone:true,imports:[RouterLink,ReactiveFormsModule,DatePipe,PurchaseReceiptFinance],templateUrl:'./purchases.html'})
export class Purchases {
  @ViewChild(PurchaseReceiptFinance) receiptFinance?:PurchaseReceiptFinance;readonly financeSource=signal<{orderId:string;receiptId:string}|null>(null);
  readonly api=inject(Api);private fb=inject(FormBuilder);private sequence=0;
  readonly orders=signal<Page<PurchaseHeader>>(empty());readonly detail=signal<PurchaseOrder|null>(null);readonly receipts=signal<Page<PurchaseReceipt>>(empty());
  readonly branches=signal<Page<Branch>>(empty());readonly suppliers=signal<Page<Supplier>>(empty());readonly products=signal<Page<Product>>(empty());readonly warehouses=signal<Page<Warehouse>>(empty());
  readonly chosenSupplier=signal<Supplier|null>(null);readonly chosenProduct=signal<Product|null>(null);
  readonly draft=signal<{product:Product;quantity:string;unitCost:string}[]>([]);readonly received=signal<Partial<Record<string,string>>>({});
  readonly busy=signal(false);readonly loading=signal(false);readonly error=signal('');readonly message=signal('');readonly pending=signal<Pending|null>(null);
  readonly header=this.fb.nonNullable.group({branchId:['',Validators.required],notes:['',Validators.maxLength(500)]});
  readonly supplierSearch=this.fb.nonNullable.group({q:['',Validators.maxLength(200)]});readonly productSearch=this.fb.nonNullable.group({q:['',Validators.maxLength(200)]});
  readonly item=this.fb.nonNullable.group({quantity:['',[Validators.required,Validators.pattern('[0-9]{1,13}(\\.[0-9]{1,6})?')]],unitCost:['',[Validators.required,Validators.pattern('[0-9]{1,13}(\\.[0-9]{1,6})?')]]});
  readonly receiptForm=this.fb.nonNullable.group({warehouseId:['',Validators.required],note:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]]});
  readonly cancelForm=this.fb.nonNullable.group({note:['',[Validators.required,Validators.minLength(5),Validators.maxLength(500)]]});
  constructor(){this.loadOrders(0);if(this.api.can('branch:read'))this.loadBranches(0);if(this.api.can('supplier:read'))this.loadSuppliers(0);if(this.api.can('product:read'))this.loadProducts(0);if(this.api.can('inventory:read'))this.loadWarehouses(0);}
  @HostListener('window:beforeunload',['$event']) beforeUnload(event:BeforeUnloadEvent){if(this.pending()){event.preventDefault();event.returnValue='';}}
  canLeave(){if(this.receiptFinance&&!this.receiptFinance.canLeave())return false;if(!this.pending())return true;this.error.set('Conclua ou repita o comando pendente antes de sair.');return false;}
  blocked(){return this.busy() || !!this.pending()||!!this.receiptFinance?.pending()||!!this.receiptFinance?.busy();}
  state(value:string){return ({OPEN:'Aberto',PARTIAL:'Recebido parcialmente',RECEIVED:'Recebido',CANCELLED:'Cancelado'} as Record<string,string>)[value] ?? value;}
  canReceive(){return !!this.detail() && ['OPEN','PARTIAL'].includes(this.detail()!.header.status);}
  private fail(error:HttpErrorResponse){this.error.set(errorMessage(error));}
  loadOrders(page:number){if(this.blocked())return;this.api.purchaseOrders(page).subscribe({next:data=>this.orders.set(data),error:e=>this.fail(e)});}
  loadBranches(page:number){if(this.blocked())return;this.api.branches(page).subscribe({next:data=>{this.branches.set(data);this.header.controls.branchId.setValue('');},error:e=>this.fail(e)});}
  loadSuppliers(page:number){if(this.blocked() || this.supplierSearch.invalid)return;this.api.suppliers(this.supplierSearch.controls.q.value,page).subscribe({next:data=>this.suppliers.set(data),error:e=>this.fail(e)});}
  loadProducts(page:number){if(this.blocked() || this.productSearch.invalid)return;this.api.products(this.productSearch.controls.q.value,page).subscribe({next:data=>this.products.set(data),error:e=>this.fail(e)});}
  loadWarehouses(page:number){if(this.blocked())return;this.api.warehouses(page).subscribe({next:data=>{this.warehouses.set(data);this.receiptForm.controls.warehouseId.setValue('');},error:e=>this.fail(e)});}
  pickSupplier(supplier:Supplier){if(!this.blocked() && supplier.active)this.chosenSupplier.set(supplier);}
  pickProduct(product:Product){if(!this.blocked() && product.active)this.chosenProduct.set(product);}
  addItem(){
    const product=this.chosenProduct();if(this.blocked() || !product || this.item.invalid)return;
    const input=this.item.getRawValue();if(!/[1-9]/.test(input.quantity)){this.error.set('Quantidade deve ser positiva.');return;}
    if(this.draft().some(line=>line.product.id===product.id)){this.error.set('Produto já adicionado. Remova o item para alterar.');return;}
    if(this.draft().length>=100){this.error.set('O pedido aceita até 100 produtos.');return;}
    this.draft.update(lines=>[...lines,{product,...input}]);this.chosenProduct.set(null);this.item.reset();this.error.set('');
  }
  removeItem(id:string){if(!this.blocked())this.draft.update(lines=>lines.filter(line=>line.product.id!==id));}
  create(){
    const supplier=this.chosenSupplier();if(this.blocked() || this.header.invalid || !supplier || !this.draft().length)return;
    this.pending.set({kind:'create',input:{...this.header.getRawValue(),supplierId:supplier.id,requestId:crypto.randomUUID(),items:this.draft().map(line=>({productId:line.product.id,quantity:line.quantity,unitCost:line.unitCost}))}});this.sendPending();
  }
  open(id:string){
    if(this.blocked())return;this.financeSource.set(null);const sequence=++this.sequence;this.detail.set(null);this.receipts.set(empty());this.received.set({});this.receiptForm.reset();this.cancelForm.reset();this.loading.set(true);
    forkJoin({order:this.api.purchaseOrder(id),receipts:this.api.purchaseReceipts(id,0)}).pipe(finalize(()=>{if(sequence===this.sequence)this.loading.set(false);})).subscribe({next:data=>{if(sequence===this.sequence){this.detail.set(data.order);this.receipts.set(data.receipts);}},error:e=>{if(sequence===this.sequence)this.fail(e);}});
  }
  loadReceipts(page:number){const order=this.detail();if(!order || this.blocked())return;const sequence=this.sequence;this.api.purchaseReceipts(order.header.id,page).subscribe({next:data=>{if(sequence===this.sequence)this.receipts.set(data);},error:e=>{if(sequence===this.sequence)this.fail(e);}});}
  financeReceipt(receipt:PurchaseReceipt){const order=this.detail();if(this.blocked()||!order)return;this.financeSource.set({orderId:order.header.id,receiptId:receipt.id});}

  setReceived(id:string,value:string){if(!this.blocked())this.received.update(values=>({...values,[id]:value}));}
  receive(){
    const order=this.detail();if(!order || this.blocked() || this.receiptForm.invalid)return;
    const items=Object.entries(this.received()).flatMap(([orderLineId,quantity])=>quantity?[{orderLineId,quantity}]:[]);
    if(!items.length || items.some(item=>!/^\d{1,13}(\.\d{1,6})?$/.test(item.quantity) || !/[1-9]/.test(item.quantity))){this.error.set('Informe quantidades positivas, com até seis casas decimais.');return;}
    this.pending.set({kind:'receive',id:order.header.id,input:{...this.receiptForm.getRawValue(),receiptId:crypto.randomUUID(),items}});this.sendPending();
  }
  cancel(){const order=this.detail();if(!order || this.blocked() || this.cancelForm.invalid)return;this.pending.set({kind:'cancel',id:order.header.id,input:{version:order.header.version,note:this.cancelForm.controls.note.value}});this.sendPending();}
  private clearPending(){this.pending.set(null);this.api.inventoryPending.set(false);this.header.enable();this.item.enable();this.receiptForm.enable();this.cancelForm.enable();}
  sendPending(){
    const command=this.pending();if(!command || this.busy())return;
    this.busy.set(true);this.error.set('');this.message.set('');this.api.inventoryPending.set(true);this.header.disable();this.item.disable();this.receiptForm.disable();this.cancelForm.disable();
    const request:Observable<unknown>=command.kind==='create'?this.api.createPurchaseOrder(command.input):command.kind==='receive'?this.api.receivePurchase(command.id,command.input):this.api.cancelPurchase(command.id,command.input);
    request.subscribe({next:response=>{
      this.busy.set(false);this.clearPending();this.message.set(command.kind==='create'?'Pedido criado.':command.kind==='receive'?'Recebimento registrado e estoque atualizado.':'Saldo pendente cancelado; mercadorias recebidas preservadas.');
      if(command.kind==='create'){this.header.reset();this.draft.set([]);this.chosenSupplier.set(null);this.open((response as PurchaseOrder).header.id);}else this.open(command.id);
      this.loadOrders(0);
    },error:(error:HttpErrorResponse)=>{this.busy.set(false);this.fail(error);if(error.status!==0 && error.status<500)this.clearPending();else this.message.set('Resultado ainda incerto. Repita o mesmo comando para confirmar sem duplicar.');}});
  }
}
