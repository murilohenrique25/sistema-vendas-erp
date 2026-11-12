import { Component, HostListener, ViewChild, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ProductEditor } from './product-editor';
import { Api, Barcode, Page, Product, errorMessage } from './api';

@Component({ standalone: true, imports: [ReactiveFormsModule,ProductEditor], template: `
  <p class="eyebrow">CATÁLOGO INTERNO</p><h1>Produtos</h1><p>Cadastre seus produtos e localize-os pelo código de barras.</p>
  <section class="card"><h2>Leitura de código</h2><form class="inline-form" [formGroup]="scanner" (ngSubmit)="lookup()"><label>Tipo<select formControlName="kind"><option value="GTIN">EAN / UPC / GTIN</option><option value="INTERNAL">Código interno</option></select></label><label>Código de barras<input formControlName="code" autocomplete="off" placeholder="Leia ou digite o código" required></label><button [disabled]="scanner.invalid || looking()||blocked()">{{looking() ? 'Consultando…' : 'Localizar'}}</button></form>
  @if (found()) { <p class="success" role="status">{{found()?.name}} · {{found()?.sku}} · {{money(found()!.salePrice)}}</p> }</section>
  @if (error()) { <p class="error" role="alert">{{error()}}</p> } @if (message()) { <p class="success" role="status">{{message()}}</p> }
  @if (api.can('product:write')) { <section class="card"><h2>Novo produto</h2><form [formGroup]="form" (ngSubmit)="create()" class="product-form"><label>SKU<input formControlName="sku" maxlength="60" required></label><label>Nome<input formControlName="name" maxlength="200" required></label><label>Unidade<select formControlName="unit">@for (unit of units; track unit) { <option [value]="unit">{{unit}}</option> }</select></label><label>Preço de venda (R$)<input formControlName="salePrice" inputmode="decimal" placeholder="0.00" aria-describedby="price-help" required><small id="price-help">Use ponto decimal, por exemplo 12.50.</small></label><label>Descrição<textarea formControlName="description" maxlength="2000" rows="2"></textarea></label><label>Códigos de barras<textarea formControlName="codes" rows="2" aria-describedby="barcode-help"></textarea><small id="barcode-help">Um por linha, até 20. GTINs têm dígito verificador validado.</small></label><label>Tipo dos códigos<select formControlName="kind"><option value="GTIN">EAN / UPC / GTIN</option><option value="INTERNAL">Códigos internos</option></select></label><button [disabled]="form.invalid || saving()||blocked()||!!selected()||editorLoading()">{{saving() ? 'Salvando…' : 'Cadastrar produto'}}</button></form></section> }
  <section class="card"><div class="section-header"><h2>Catálogo</h2><span>{{data().total}} produtos</span></div><form class="search-form" [formGroup]="search" (ngSubmit)="load(0)"><label>Nome, SKU ou código parcial<input formControlName="q" maxlength="200"></label><label>Situação<select formControlName="active"><option value="">Todos</option><option value="true">Ativos</option><option value="false">Inativos</option></select></label><button [disabled]="loading()||blocked()">Pesquisar</button></form>
  @if (loading()) { <p role="status">Carregando produtos…</p> } @else { <div class="table-scroll"><table><thead><tr><th>SKU</th><th>Produto</th><th>Unidade</th><th>Preço de venda</th><th>Situação / edição</th></tr></thead><tbody>@for (product of data().items; track product.id) { <tr><td class="mono">{{product.sku}}</td><td>{{product.name}}<small class="code-list">@for (barcode of product.barcodes; track barcode.code) { <span>{{barcode.code}} </span> }</small></td><td>{{product.unit}}</td><td>{{money(product.salePrice)}}</td><td>{{product.active?'Ativo':'Inativo'}} @if(api.can('product:write')){<button class="secondary" (click)="edit(product)" [disabled]="blocked()||saving()||editorLoading()">Editar</button>}</td></tr> } @empty { <tr><td colspan="5">Nenhum produto encontrado. Cadastre o primeiro produto da empresa.</td></tr> }</tbody></table></div> }
  <div class="pagination"><span>Página {{page()+1}}</span><button class="secondary" (click)="load(page()-1)" [disabled]="page()===0 || loading()||blocked()">Anterior</button><button class="secondary" (click)="load(page()+1)" [disabled]="(page()+1)*20>=data().total || loading()||blocked()">Próxima</button></div></section><erp-product-editor [product]="selected()" [externalBlocked]="editorLoading()||saving()" (changed)="updated($event)" (closed)="selected.set(null)" />` })
export class Products {
  @ViewChild(ProductEditor) editor?:ProductEditor;readonly selected=signal<Product|null>(null);readonly editorLoading=signal(false);private loadSequence=0;
  readonly api = inject(Api); private fb = inject(FormBuilder);
  readonly units = ['UN', 'CX', 'PCT', 'KG', 'G', 'L', 'ML', 'M', 'M2'];
  readonly data = signal<Page<Product>>({items: [], total: 0, page: 0, size: 20});
  readonly page = signal(0); readonly loading = signal(false); readonly saving = signal(false); readonly looking = signal(false);
  readonly error = signal(''); readonly message = signal(''); readonly found = signal<Product | null>(null);
  readonly scanner = this.fb.nonNullable.group({code: ['', [Validators.required, Validators.pattern('[A-Za-z0-9._-]{1,60}')]], kind: this.fb.nonNullable.control<Barcode['kind']>('GTIN')});
  readonly search = this.fb.nonNullable.group({q: ['', Validators.maxLength(200)],active:['']});
  readonly form = this.fb.nonNullable.group({sku: ['', [Validators.required, Validators.pattern('[A-Za-z0-9._-]{1,60}')]], name: ['', [Validators.required, Validators.maxLength(200)]], description: ['', Validators.maxLength(2000)], unit: ['UN', Validators.required], salePrice: ['0.00', [Validators.required, Validators.pattern('[0-9]{1,17}(\\.[0-9]{1,2})?')]], codes: ['', Validators.maxLength(1220)], kind: this.fb.nonNullable.control<Barcode['kind']>('GTIN')});
  constructor() { this.load(0); }
  blocked(){return !!this.editor?.blocked();}
  canLeave(){return this.editor?.canLeave()??true;}
  @HostListener('window:beforeunload',['$event']) unload(event:BeforeUnloadEvent){if(this.blocked()){event.preventDefault();event.returnValue='';}}
  edit(p:Product){if(this.blocked()||this.saving()||this.editorLoading())return;this.editorLoading.set(true);this.api.product(p.id).subscribe({next:v=>{this.editorLoading.set(false);this.selected.set(v);},error:e=>{this.editorLoading.set(false);this.error.set(errorMessage(e));}});}
  updated(p:Product){if(this.found()?.id===p.id)this.found.set(p);this.load(this.page());}
  money(value: string) { const [integer, decimals = ''] = value.split('.'); return `R$ ${integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.')},${decimals.padEnd(2, '0')}`; }
  load(page: number) {
    if (this.search.invalid||this.blocked()) return;
    const sequence=++this.loadSequence;this.loading.set(true); this.error.set('');
    this.api.products(this.search.controls.q.value, page,this.search.controls.active.value===''?undefined:this.search.controls.active.value==='true').subscribe({next:data=>{if(sequence===this.loadSequence){this.loading.set(false);this.data.set(data);this.page.set(page);}},error:e=>{if(sequence===this.loadSequence){this.loading.set(false);this.error.set(errorMessage(e));}}});
  }
  lookup() {
    if (this.scanner.invalid || this.looking()||this.blocked()) return;
    this.looking.set(true); this.error.set(''); this.message.set(''); this.found.set(null);
    const {code, kind} = this.scanner.getRawValue();
    this.api.lookupProduct(code, kind).pipe(finalize(() => this.looking.set(false))).subscribe({next: product => this.found.set(product), error: e => {
      if (e.status === 404 && this.api.can('product:write')) { this.form.patchValue({codes: code, kind}); this.message.set('Código não encontrado. Preencha o cadastro abaixo; o código já foi incluído.'); } else this.error.set(errorMessage(e));
    }});
  }
  create() {
    if (this.form.invalid || this.saving()||this.blocked()||this.selected()||this.editorLoading()) return;
    this.saving.set(true); this.error.set(''); this.message.set('');
    const {codes, kind, ...input} = this.form.getRawValue();
    const barcodes = codes.split(/[\r\n]+/).map(code => code.trim()).filter(Boolean).map(code => ({code, kind}));
    this.api.createProduct({...input, barcodes}).pipe(finalize(() => this.saving.set(false))).subscribe({next: () => {this.form.reset({unit: 'UN', salePrice: '0.00', kind: 'GTIN'}); this.message.set('Produto cadastrado na base interna.'); this.load(0);}, error: e => this.error.set(errorMessage(e))});
  }
}
