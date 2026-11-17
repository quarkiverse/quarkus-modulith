import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/button';

/**
 * Lists the incomplete event publications of the registry and submits them again.
 */
export class QwcModulithPublications extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
        }
        .toolbar {
            display: flex;
            gap: 1em;
            padding: 0.5em;
        }
        code {
            font-size: 85%;
        }
    `;

    static properties = {
        _publications: { state: true },
        _message: { state: true }
    };

    constructor() {
        super();
        this._publications = [];
        this._message = '';
    }

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getIncompletePublications().then(response => {
            this._publications = response.result;
        }).catch(error => this._failed('load the publications', error));
    }

    _resubmit(id) {
        this.jsonRpc.resubmit({ id: id }).then(response => {
            this._message = `Resubmitted ${response.result} publication(s)`;
            this._load();
        }).catch(error => this._failed('resubmit the publication', error));
    }

    _resubmitAll() {
        this.jsonRpc.resubmitAll().then(response => {
            this._message = `Resubmitted ${response.result} publication(s)`;
            this._load();
        }).catch(error => this._failed('resubmit the publications', error));
    }

    _failed(action, error) {
        this._message = `Could not ${action}: ${error?.error?.message ?? error?.message ?? error}`;
    }

    render() {
        return html`
            <div class="toolbar">
                <vaadin-button @click="${() => this._load()}">Refresh</vaadin-button>
                <vaadin-button theme="primary" @click="${() => this._resubmitAll()}">Resubmit all</vaadin-button>
                <span>${this._message}</span>
            </div>
            <vaadin-grid .items="${this._publications}" theme="no-border row-stripes">
                <vaadin-grid-sort-column header="Published" path="publicationDate" auto-width></vaadin-grid-sort-column>
                <vaadin-grid-sort-column header="Status" path="status" auto-width></vaadin-grid-sort-column>
                <vaadin-grid-column header="Event" auto-width
                    ${columnBodyRenderer((p) => html`<code>${p.eventType}</code>`, [])}></vaadin-grid-column>
                <vaadin-grid-column header="Listener" auto-width
                    ${columnBodyRenderer((p) => html`<code>${p.listenerId}</code>`, [])}></vaadin-grid-column>
                <vaadin-grid-sort-column header="Attempts" path="completionAttempts" auto-width></vaadin-grid-sort-column>
                <vaadin-grid-column header="" auto-width
                    ${columnBodyRenderer((p) => html`<vaadin-button theme="small"
                        @click="${() => this._resubmit(p.id)}">Resubmit</vaadin-button>`, [])}></vaadin-grid-column>
            </vaadin-grid>`;
    }
}
customElements.define('qwc-modulith-publications', QwcModulithPublications);
