import {useCallback, useMemo} from "react";
import {Chart as ChartJS} from "chart.js";

export const useChartOptions = ({
                                    chartData,
                                    dataPerEpisode,
                                    episodes,
                                    ZOOM_OPTIONS,
                                    openEpisodeModal,
                                }) => {
    const handleTooltipLabelColor = useCallback((tooltipItem) => {
        const datasetIndex = tooltipItem.datasetIndex;
        const borderColor = chartData?.datasets?.[datasetIndex]?.borderColor;
        return {borderColor, backgroundColor: borderColor};
    }, [chartData]);

    const handleTooltipLabel = useCallback((context) => {
        const datasetLabel = context.dataset.label || "";
        const value = context.raw;

        if (datasetLabel.includes("Avg.")) {
            return `${datasetLabel}: ${value.toFixed(2)}`;
        }

        const episodeIndex = context.dataIndex;
        const totalVotes = dataPerEpisode[episodeIndex]?.totalVotes || 0;
        const votes = value;
        const percentage =
            totalVotes > 0
                ? ((votes / totalVotes) * 100).toFixed(1)
                : "0.0";

        return `${datasetLabel}: ${votes} votes (${percentage}%)`;
    }, [dataPerEpisode]);

    const handleItemSort = useCallback((a, b) => b.datasetIndex - a.datasetIndex, []);

    const handleGenerateLabels = useCallback((chart) => {
        const labels = ChartJS.defaults.plugins.legend.labels.generateLabels(chart);
        return labels.reverse().map((label) => {
            label.fillStyle = label.strokeStyle;
            label.fontColor = label.hidden
                ? "rgba(255, 255, 255, 0.4)"
                : "rgba(255, 255, 255, 0.8)";
            label.lineWidth = label.hidden ? 0 : 5;
            return label;
        });
    }, []);

    const handleY1TickCallback = useCallback((value) => value.toFixed(2), []);

    const handleChartClick = useCallback((event, elements) => {
        if (elements.length > 0) {
            const element = elements[0];
            const episodeIndex = element.index;
            const targetEpisode = episodes[episodeIndex];
            if (targetEpisode !== undefined) {
                openEpisodeModal(targetEpisode);
            }
        }
    }, [episodes, openEpisodeModal]);

    return useMemo(() => {
        const chartPlugins = {
            tooltip: {
                animation: {duration: 0},
                position: "nearest",
                backgroundColor: "rgba(0, 0, 0, 0.9)",
                itemSort: handleItemSort,
                titleFont: {size: 10},
                bodyFont: {size: 10},
                callbacks: {
                    labelColor: handleTooltipLabelColor,
                    label: handleTooltipLabel,
                },
            },
            legend: {
                position: "top",
                labels: {
                    color: "#ffffff",
                    boxHeight: 7,
                    usePointStyle: true,
                    generateLabels: handleGenerateLabels,
                },
            },
        };

        chartPlugins['zoom'] = ZOOM_OPTIONS;

        return {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: "index",
                intersect: false,
            },
            plugins: chartPlugins,
            scales: {
                x: {
                    stacked: true,
                    ticks: {
                        color: "rgba(192, 192, 192, 0.57)",
                        autoSkipPadding: 10,
                        maxRotation: 0,
                        minRotation: 0,
                    },
                },
                y: {
                    position: "right",
                    stacked: true,
                    ticks: {color: "rgba(192, 192, 192, 0.57)"},
                },
                y1: {
                    position: "left",
                    beginAtZero: false,
                    ticks: {
                        color: "rgba(192, 192, 192, 0.57)",
                        callback: handleY1TickCallback,
                    },
                },
            },
            animation: {duration: 600},
            onClick: handleChartClick,
        };
    }, [
        ZOOM_OPTIONS,
        handleItemSort,
        handleTooltipLabelColor,
        handleTooltipLabel,
        handleGenerateLabels,
        handleY1TickCallback,
        handleChartClick
    ]);
};