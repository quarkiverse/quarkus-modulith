import { LitElement, html, css } from 'lit';
import { violations } from 'build-time-data';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';

/**
 * Shows the module violations found by the last build. With the default configuration the build fails instead, so
 * this page is mostly useful with quarkus.modulith.verification.mode=warn.
 */
export class QwcModulithViolations extends LitElement {

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
        }
        .ok {
            padding: 10px;
        }
        pre {
            white-space: pre-wrap;
            margin: 0;
        }
    `;

    render() {
        if (violations.length === 0) {
            return html`<div class="ok">The modules follow their rules. No violations.</div>`;
        }
        return html`
            <vaadin-grid .items="${violations}" theme="no-border row-stripes wrap-cell-content">
                <vaadin-grid-sort-column header="Module" path="module" auto-width></vaadin-grid-sort-column>
                <vaadin-grid-sort-column header="Rule" path="kind" auto-width></vaadin-grid-sort-column>
                <vaadin-grid-column header="Details"
                    ${columnBodyRenderer((v) => html`<pre>${v.message}</pre>`, [])}></vaadin-grid-column>
            </vaadin-grid>`;
    }
}
customElements.define('qwc-modulith-violations', QwcModulithViolations);
