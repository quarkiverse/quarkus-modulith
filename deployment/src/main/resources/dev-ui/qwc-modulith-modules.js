import { LitElement, html, css } from 'lit';
import { modules, basePackage } from 'build-time-data';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import 'qui-badge';

/**
 * Lists the application modules with their dependencies and API.
 */
export class QwcModulithModules extends LitElement {

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
        }
        .header {
            padding: 0 10px;
            color: var(--lumo-contrast-60pct);
        }
        .grid {
            height: 100%;
        }
        code {
            font-size: 85%;
        }
        .list {
            display: flex;
            flex-direction: column;
            gap: 2px;
        }
    `;

    render() {
        return html`
            <div class="header">Base package <code>${basePackage}</code></div>
            <vaadin-grid .items="${modules}" class="grid" theme="no-border row-stripes">
                <vaadin-grid-sort-column header="Module" path="name" auto-width
                    ${columnBodyRenderer(this._nameRenderer, [])}></vaadin-grid-sort-column>
                <vaadin-grid-column header="Base package" auto-width
                    ${columnBodyRenderer((m) => html`<code>${m.basePackage}</code>`, [])}></vaadin-grid-column>
                <vaadin-grid-column header="Depends on" auto-width
                    ${columnBodyRenderer((m) => this._list(m.dependencies), [])}></vaadin-grid-column>
                <vaadin-grid-column header="Allowed" auto-width
                    ${columnBodyRenderer((m) => this._list(m.allowedDependencies), [])}></vaadin-grid-column>
                <vaadin-grid-column header="Named interfaces" auto-width
                    ${columnBodyRenderer((m) => this._list(m.namedInterfaces), [])}></vaadin-grid-column>
                <vaadin-grid-column header="Publishes" auto-width
                    ${columnBodyRenderer((m) => this._list(m.publishedEvents), [])}></vaadin-grid-column>
                <vaadin-grid-column header="Listens to" auto-width
                    ${columnBodyRenderer((m) => this._list(m.listenedEvents), [])}></vaadin-grid-column>
            </vaadin-grid>`;
    }

    _nameRenderer(module) {
        const badge = module.type === 'open'
            ? html`<qui-badge small><span>open</span></qui-badge>`
            : html``;
        return html`<strong>${module.displayName}</strong> ${badge}`;
    }

    _list(values) {
        if (!values || values.length === 0) {
            return html`<span>none</span>`;
        }
        return html`<div class="list">${values.map((v) => html`<code>${v}</code>`)}</div>`;
    }
}
customElements.define('qwc-modulith-modules', QwcModulithModules);
