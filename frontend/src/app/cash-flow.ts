import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Api, Branch, CashFlowReport, Page, errorMessage } from './api';
@Component({standalone:true,imports:[ReactiveFormsModule,DatePipe],templateUrl:'./cash-flow.html'})
export class CashFlow {
 readonly api=inject(Api);private fb=inject(FormBuilder);private sequence=0;
 readonly report=signal<CashFlowReport|null>(null);readonly branches=signal<Page<Branch>>({items:[],total:0,page:0,size:20});readonly selectedBranch=signal<Branch|null>(null);readonly busy=signal(false);readonly error=signal('');
 readonly form=this.fb.nonNullable.group({from:['',Validators.required],to:['',Validators.required],branchId:['']});
 constructor(){if(this.api.can('branch:read'))this.loadBranches(0);this.api.cashFlowCalendar().subscribe({next:v=>{const date=new Date(v.date+'T00:00:00Z');date.setUTCDate(date.getUTCDate()+30);this.form.patchValue({from:v.date,to:date.toISOString().slice(0,10)});this.load();},error:e=>this.error.set(errorMessage(e))});}
 date(value:string){return value.split('-').reverse().join('/');}
 selectBranch(){this.selectedBranch.set(this.branches().items.find(b=>b.id===this.form.controls.branchId.value)??null);}
 branchVisible(id:string){return this.branches().items.some(b=>b.id===id);}
 loadBranches(page:number){if(this.busy())return;this.api.branches(page).subscribe({next:v=>this.branches.set(v),error:e=>this.error.set(errorMessage(e))});}
 load(){if(this.form.invalid || this.busy())return;const v=this.form.getRawValue();if(v.from>v.to){this.error.set('O início deve ser anterior ou igual ao fim.');return;}const sequence=++this.sequence;this.busy.set(true);this.form.disable();this.error.set('');this.report.set(null);this.api.cashFlow(v.from,v.to,v.branchId).pipe(finalize(()=>{if(sequence===this.sequence){this.busy.set(false);this.form.enable();}})).subscribe({next:r=>{if(sequence===this.sequence)this.report.set(r);},error:e=>{if(sequence===this.sequence)this.error.set(errorMessage(e));}});}
}
