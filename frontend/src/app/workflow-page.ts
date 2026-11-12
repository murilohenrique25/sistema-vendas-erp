import { Directive, HostListener, Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Api, errorMessage } from './api';

@Injectable()
export class WorkflowClient {
 readonly api=inject(Api);readonly http=inject(HttpClient);readonly busy=signal(false);readonly pending=signal<{method:string;url:string;body:unknown}|null>(null);readonly error=signal('');readonly message=signal('');private success:((result:any)=>void)|null=null;
 blocked(){return this.busy()||!!this.pending();}
 execute(method:string,url:string,body:unknown,success:(result:any)=>void){if(this.blocked())return;this.success=success;this.pending.set({method,url,body:structuredClone(body)});this.send();}
 send(){const command=this.pending();if(!command||this.busy())return;this.busy.set(true);this.api.inventoryPending.set(true);this.error.set('');this.http.request(command.method,command.url,{body:command.body}).subscribe({next:result=>{this.busy.set(false);this.pending.set(null);this.api.inventoryPending.set(false);this.message.set('Operação registrada.');const callback=this.success;this.success=null;callback?.(result);},error:(e:HttpErrorResponse)=>{this.busy.set(false);this.error.set(errorMessage(e));if(e.status!==0&&e.status<500){this.pending.set(null);this.success=null;this.api.inventoryPending.set(false);}else this.message.set('Resultado incerto. Repita a mesma operação antes de editar ou sair.');}});}
}
@Directive()
export class WorkflowPage {
 readonly op=inject(WorkflowClient);readonly api=this.op.api;
 canLeave(){return !this.op.blocked();}
 @HostListener('window:beforeunload',['$event']) unload(e:BeforeUnloadEvent){if(this.op.blocked()){e.preventDefault();e.returnValue='';}}
}
