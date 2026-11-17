import { LitElement, html, css } from 'lit';
import { listeners } from 'build-time-data';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';

/**
 * Lists the @ApplicationModuleListener methods and the events they receive.
 */
export class QwcModulithListeners extends LitElement {

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
            <vaadin-grid .items="${listeners}" theme="no-border row-stripes">
                <vaadin-grid-sort-column header="Module" path="module" auto-width></vaadin-grid-sort-column>
                <vaadin-grid-column header="Event" auto-width
                    ${columnBodyRenderer((l) => html`<code>${l.eventType}</code>`, [])}></vaadin-grid-column>
                <vaadin-grid-column header="Listener" auto-width
                    ${columnBodyRenderer((l) => html`<code>${l.id}</code>`, [])}></vaadin-grid-column>
                <vaadin-grid-column header="Delivery" auto-width
                    ${columnBodyRenderer((l) => html`${l.async ? 'async' : 'sync'}`, [])}></vaadin-grid-column>
            </vaadin-grid>`;
    }
}
customElements.define('qwc-modulith-listeners', QwcModulithListeners);
