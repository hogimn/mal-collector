import {
    BarElement,
    CategoryScale,
    Chart,
    Filler,
    Legend,
    LinearScale,
    LineController,
    LineElement,
    PointElement,
    TimeScale,
    Title,
    Tooltip,
} from "chart.js";
import zoomPlugin from "chartjs-plugin-zoom";

const handleBeforeInit = (chart) => {
    const originalFit = chart.legend.fit;

    chart.legend.fit = function fit() {
        originalFit.bind(chart.legend)();
        this.height += 10;
    };
};

const handleAfterDatasetsDraw = (chart, _args, _plugins) => {
    const {
        ctx,
        tooltip,
        chartArea: {top, bottom},
    } = chart;

    if (tooltip._active.length > 0) {
        const tooltipX = tooltip._active[0].element.x;
        const tooltipY = tooltip._active[0].element.y;
        const datasetIndex = tooltip._active[0].datasetIndex;
        const dataset = chart.data.datasets[datasetIndex];

        ctx.save();
        ctx.beginPath();
        ctx.lineWidth = 1;
        ctx.strokeStyle = "rgba(173, 173, 173, 0.95)";
        ctx.setLineDash([2, 2]);
        ctx.moveTo(tooltipX, top);
        ctx.lineTo(tooltipX, bottom);
        ctx.stroke();
        ctx.closePath();

        ctx.beginPath();
        ctx.setLineDash([]);
        ctx.arc(tooltipX, tooltipY, 4, 0, Math.PI * 2);
        ctx.fillStyle = dataset.borderColor;
        ctx.fill();
        ctx.closePath();
    }
};

const handleTooltipPosition = (elements, position) => {
    const chartWidth = elements[0]?.element?.$context?.chart?.width || 0;

    const offsetX = 100;
    const isLeftSide = position.x < chartWidth / 2;

    return {
        x: isLeftSide ? position.x + offsetX : position.x - offsetX,
        y: 0,
    };
};

const increaseLegendSpacing = {
    id: "increase-legend-spacing",
    beforeInit: handleBeforeInit,
};

const verticalHoverLine = {
    id: "verticalHoverLine",
    afterDatasetsDraw: handleAfterDatasetsDraw,
};

export const registerCharts = () => {
    Chart.register(
        CategoryScale,
        LinearScale,
        PointElement,
        LineElement,
        Title,
        Tooltip,
        Legend,
        zoomPlugin,
        TimeScale,
        BarElement,
        Filler,
        increaseLegendSpacing,
        verticalHoverLine,
        LineController
    );

    Tooltip.positioners.poll = handleTooltipPosition;

    Chart.defaults.font.size = 11;
};