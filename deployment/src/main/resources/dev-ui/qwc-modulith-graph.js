import { LitElement, html, css } from 'lit';
import { modules, relations } from 'build-time-data';
import 'echarts-force-graph';

/**
 * Draws the modules and their dependencies as a force graph.
 */
export class QwcModulithGraph extends LitElement {

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            padding: 10px;
        }
        .legend {
            color: var(--lumo-contrast-60pct);
        }
    `;

    render() {
        const categories = ['closed module', 'open module'];
        const colors = ['#4695eb', '#91cc75'];
        const nodes = modules.map((m) => ({
            name: m.displayName,
            value: 10 + m.classCount,
            symbolSize: Math.min(60, 20 + m.classCount),
            category: m.type === 'open' ? 1 : 0
        }));
        const index = new Map(modules.map((m, i) => [m.name, i]));
        const links = relations.map((r) => ({
            source: index.get(r.source),
            target: index.get(r.target),
            value: r.type
        }));
        return html`
            <span class="legend">Arrows point from a module to the modules it uses or listens to.</span>
            <echarts-force-graph width="800px" height="600px"
                edgeLength="200"
                repulsion="400"
                categories="${JSON.stringify(categories)}"
                colors="${JSON.stringify(colors)}"
                nodes="${JSON.stringify(nodes)}"
                links="${JSON.stringify(links)}">
            </echarts-force-graph>`;
    }
}
customElements.define('qwc-modulith-graph', QwcModulithGraph);
