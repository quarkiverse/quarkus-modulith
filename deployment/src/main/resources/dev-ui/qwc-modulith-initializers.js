import { LitElement, html, css } from 'lit';
import { initializers } from 'build-time-data';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';

/**
 * Lists the ApplicationModuleInitializer beans per module.
 */
export class QwcModulithInitializers extends LitElement {

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
        }
        code {
            font-size: 85%;
        }
    `;

    render() {
        return html`
            <vaadin-grid .items="${initializers}" theme="no-border row-stripes">
                <vaadin-grid-sort-column header="Module" path="module" auto-width></vaadin-grid-sort-column>
                <vaadin-grid-column header="Initializer" auto-width
                    ${columnBodyRenderer((i) => html`<code>${i.type}</code>`, [])}></vaadin-grid-column>
            </vaadin-grid>`;
    }
}
customElements.define('qwc-modulith-initializers', QwcModulithInitializers);
