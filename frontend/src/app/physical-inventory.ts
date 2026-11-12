import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api, Page, Product, Warehouse, errorMessage } from './api';
import { WorkflowClient, WorkflowPage } from './workflow-page';
interface Line {productId:string;sku:string;name:string;unit:string;expectedQuantity:string;countedQuantity:string|null;difference:string|null;unitCost:string|null;operationId:string|null}
interface Count {id:string;warehouseId:string;branchId:string;status:string;version:number;note:string;createdBy:string;lines:Line[];reviewNote:string|null}
@Component({standalone:true,imports:[FormsModule],providers:[WorkflowClient],templateUrl:'./physical-inventory.html'})
export class PhysicalInventory extends WorkflowPage {
 readonly warehouses=signal<Page<Warehouse>>({items:[],total:0,page:0,size:20});readonly products=signal<Page<Product>>({items:[],total:0,page:0,size:20});readonly counts=signal<Page<Count>>({items:[],total:0,page:0,size:20});readonly detail=signal<Count|null>(null);readonly chosen=signal<Product[]>([]);
 warehouseId='';note='';query='';reviewNote='';quantities:Record<string,string>={};costs:Record<string,string>={};
 constructor(){super();this.loadWarehouses(0);this.loadProducts(0);this.load(0);}
 loadWarehouses(page:number){if(!this.op.blocked())this.api.warehouses(page).subscribe({next:v=>this.warehouses.set(v),error:e=>this.op.error.set(errorMessage(e))});}
 loadProducts(page:number){if(!this.op.blocked())this.api.products(this.query,page,true).subscribe({next:v=>this.products.set(v),error:e=>this.op.error.set(errorMessage(e))});}
 load(page:number){if(!this.op.blocked())this.op.http.get<Page<Count>>('/api/v1/inventory/counts',{params:{page,size:20}}).subscribe({next:v=>this.counts.set(v),error:e=>this.op.error.set(errorMessage(e))});}
 pick(p:Product){if(!this.op.blocked()&&this.chosen().length<100&&!this.chosen().some(x=>x.id===p.id))this.chosen.update(items=>[...items,p]);}
 remove(id:string){if(!this.op.blocked())this.chosen.update(items=>items.filter(p=>p.id!==id));}
 show(v:Count){this.detail.set(v);this.quantities={};this.costs={};for(const l of v.lines){this.quantities[l.productId]=l.countedQuantity??'';this.costs[l.productId]=l.unitCost??'';}this.reviewNote='';}
 open(id:string){if(!this.op.blocked())this.op.http.get<Count>(`/api/v1/inventory/counts/${id}`).subscribe({next:v=>this.show(v),error:e=>this.op.error.set(errorMessage(e))});}
 create(){if(this.op.blocked()||!this.warehouseId||this.note.trim().length<5||!this.chosen().length)return;this.op.execute('POST','/api/v1/inventory/counts',{requestId:crypto.randomUUID(),warehouseId:this.warehouseId,note:this.note.trim(),productIds:this.chosen().map(p=>p.id)},v=>{this.show(v);this.chosen.set([]);this.load(0);});}
 save(){const h=this.detail();if(!h||this.op.blocked())return;const lines=[];for(const l of h.lines){const q=this.quantities[l.productId];if(!/^\d{1,13}(\.\d{1,6})?$/.test(q)){this.op.error.set('Informe uma contagem válida para todos os itens, inclusive zero.');return;}const cost=this.costs[l.productId];if(cost&&!/^\d{1,13}(\.\d{1,6})?$/.test(cost)){this.op.error.set('Custo inválido.');return;}lines.push({productId:l.productId,quantity:q,...(cost?{unitCost:cost}:{})});}this.op.execute('PUT',`/api/v1/inventory/counts/${h.id}/counts`,{requestId:crypto.randomUUID(),version:h.version,lines},v=>{this.show(v);this.load(0);});}
 action(action:string){const h=this.detail();if(!h||this.op.blocked()||this.reviewNote.trim().length<5)return;this.op.execute('POST',`/api/v1/inventory/counts/${h.id}/${action}`,{requestId:crypto.randomUUID(),version:h.version,note:this.reviewNote.trim()},v=>{this.show(v);this.load(0);});}
 label(s:string){return ({COUNTING:'Em contagem',SUBMITTED:'Aguardando aprovação',APPROVED:'Aprovado',CANCELLED:'Cancelado'} as Record<string,string>)[s]??s;}
}
