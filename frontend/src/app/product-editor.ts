import { Component, EventEmitter, HostListener, Input, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Api, Barcode, Product, ProductInput, errorMessage } from './api';

type Command={kind:'update';id:string;body:{version:number;product:ProductInput}}|{kind:'active';id:string;body:{version:number;active:boolean}};
@Component({selector:'erp-product-editor',standalone:true,imports:[FormsModule],templateUrl:'./product-editor.html'})
export class ProductEditor {
 readonly api=inject(Api);readonly current=signal<Product|null>(null);readonly pending=signal<Command|null>(null);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');
 @Output() changed=new EventEmitter<Product>();@Output() closed=new EventEmitter<void>();
 @Input() externalBlocked=false;
 @Input() set product(value:Product|null){if(!this.pending()&&!this.busy())this.fill(value);}
 sku='';name='';description='';price='';gtins='';internal='';
 private fill(p:Product|null){this.current.set(p);this.error.set('');this.message.set('');if(!p)return;this.sku=p.sku;this.name=p.name;this.description=p.description;this.price=p.salePrice;this.gtins=p.barcodes.filter(b=>b.kind==='GTIN').map(b=>b.code).join('\n');this.internal=p.barcodes.filter(b=>b.kind==='INTERNAL').map(b=>b.code).join('\n');}
 blocked(){return this.busy()||!!this.pending()||this.externalBlocked;}
 canLeave(){if(!this.blocked())return true;this.error.set('Confirme a operação pendente do produto antes de sair.');return false;}
 @HostListener('window:beforeunload',['$event']) unload(event:BeforeUnloadEvent){if(this.blocked()){event.preventDefault();event.returnValue='';}}
 close(){if(!this.blocked())this.closed.emit();}
 reload(){const p=this.current();if(!p||this.blocked())return;this.busy.set(true);this.api.product(p.id).subscribe({next:v=>{this.busy.set(false);this.fill(v);this.changed.emit(v);this.message.set('Cadastro atual carregado. As alterações não salvas do formulário foram substituídas.');},error:e=>{this.busy.set(false);this.error.set(errorMessage(e));}});}
 save(){const p=this.current();if(!p||this.blocked())return;const barcodes:Barcode[]=[];for(const [text,kind] of [[this.gtins,'GTIN'],[this.internal,'INTERNAL']] as const)for(const code of text.split(/[\r\n]+/).map(c=>c.trim()).filter(Boolean))barcodes.push({code,kind});
  if(!/^[A-Za-z0-9._-]{1,60}$/.test(this.sku)||!this.name.trim()||this.name.trim().length>200||this.description.trim().length>2000||!/^\d{1,17}(\.\d{1,2})?$/.test(this.price)||barcodes.length>20||barcodes.some(b=>!/^[A-Za-z0-9._-]{1,60}$/.test(b.code))){this.error.set('Confira SKU, nome, preço e códigos. Use até 20 códigos de no máximo 60 caracteres.');return;}
  this.pending.set({kind:'update',id:p.id,body:{version:p.version,product:{sku:this.sku,name:this.name.trim(),description:this.description.trim(),unit:p.unit,salePrice:this.price,barcodes}}});this.send();
 }
 toggle(){const p=this.current();if(!p||this.blocked())return;this.pending.set({kind:'active',id:p.id,body:{version:p.version,active:!p.active}});this.send();}
 send(){const p=this.pending();if(!p||this.busy())return;this.busy.set(true);this.api.inventoryPending.set(true);this.error.set('');const request=p.kind==='update'?this.api.updateProduct(p.id,p.body):this.api.setProductActive(p.id,p.body);request.subscribe({next:v=>{this.busy.set(false);this.pending.set(null);this.api.inventoryPending.set(false);if(p.kind==='update')this.fill(v);else this.current.set(v);this.message.set(p.kind==='update'?'Produto atualizado. Vendas anteriores mantêm seus dados históricos.':v.active?'Produto ativado.':'Produto desativado para novas operações que exigem produto ativo.');this.changed.emit(v);},error:(e:HttpErrorResponse)=>{this.busy.set(false);this.error.set(errorMessage(e));if(e.status!==0&&e.status<500){this.pending.set(null);this.api.inventoryPending.set(false);if(e.status===409)this.message.set('Confira o conflito. Recarregue o cadastro antes de tentar uma nova alteração.');}else this.message.set('Resultado incerto. Repita a mesma operação para conferir, sem editar os dados enviados.');}});}
}
