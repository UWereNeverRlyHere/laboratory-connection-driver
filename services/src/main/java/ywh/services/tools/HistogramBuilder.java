package ywh.services.tools;

import lombok.Getter;
import lombok.Setter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import ywh.commons.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Step-builder для генерації кількох гістограм і повернення Map&lt;name, base64&gt;.
 *
 *  HistogramBuilder.start()
 *      .addGraph("WBC", 400, points).withMarkers(lanes)
 *      .addGraph("RBC", 300, points).skipMarkers()
 *      .build();
 */
public final class HistogramBuilder {

    /* ───────────── step-інтерфейси ───────────── */
    public interface GraphStep {
        MarkerStep addGraph(String name, double max, List<String> points);
        MarkerStep addWbc(double max, List<String> points);
        MarkerStep addRbc(double max, List<String> points);
        MarkerStep addPlt(double max, List<String> points);
        Map<String, String> build();
    }

    public interface MarkerStep {
        GraphStep withMarkers(List<String> lanes, boolean lanesWithCoeff);
        GraphStep skipMarkers();                   // якщо маркери не потрібні
    }

    /* ───────────── factory-метод для старту ───────────── */
    public static GraphStep start() {
        return new Builder();
    }

    /* ───────────── внутрішня реалізація ───────────── */
    private static final class Builder implements GraphStep, MarkerStep {

        /* ------------ модель одного графіка ------------ */
        private record Task(String name, double max, List<String> points, List<String> lanes, boolean lanesWithCoeff) { }

        private final List<Task> tasks = new ArrayList<>();

        /* ------------ маркерний буфер ------------ */
        private String      tmpName;
        private double      tmpMax;
        private List<String> tmpPoints;

        /* ===== GraphStep ===== */
        @Override
        public MarkerStep addGraph(String name, double max, List<String> points) {
            Objects.requireNonNull(name);
            this.tmpName = name;
            this.tmpMax  = max;
            this.tmpPoints = points;
            return this;                    // → MarkerStep
        }

        @Override
        public MarkerStep addWbc(double max, List<String> points) {
            return addGraph("WBC",max,points);
        }

        @Override
        public MarkerStep addRbc(double max, List<String> points) {
            return addGraph("RBC",max,points);
        }

        @Override
        public MarkerStep addPlt(double max, List<String> points) {
            return addGraph("PLT",max,points);
        }

        @Override
        public Map<String, String> build() {
            LinkedHashMap<String, String> res = new LinkedHashMap<>();
            for (Task t : tasks) {
                res.put(t.name, renderGraph(t));
            }
            return res;
        }

        /* ===== MarkerStep ===== */
        @Override
        public GraphStep withMarkers(List<String> lanes, boolean lanesWithCoeff) {
            tasks.add(new Task(tmpName, tmpMax, tmpPoints, lanes, lanesWithCoeff));
            clearTmp();
            return this;                    // → GraphStep
        }

        @Override
        public GraphStep skipMarkers() {
            tasks.add(new Task(tmpName, tmpMax, tmpPoints, List.of(), false));
            clearTmp();
            return this;                    // → GraphStep
        }

        private void clearTmp() {
            tmpName = null; tmpPoints = null; tmpMax = 0;
        }

        /* ------------ логіка рендеру (адаптована з попередньої версії) ------------ */
        private final XYSeriesCollection dataset = new XYSeriesCollection();
        private XYPlot plot;
        private JFreeChart chart;
        @Setter @Getter private String color;
        @Getter private String graphName;
        private String imageString;

        private String renderGraph(Task t) {
            createChart(t.name);
            setDataset(t.points, t.max);
            double coeff = t.lanesWithCoeff ? t.max / t.points.size() : 1;
            for (String lane : t.lanes) setMarker(String.valueOf(Double.parseDouble(lane) * coeff));
            generateImage();
            return imageString;
        }

        /* --- нижче залишено майже без змін --- */
        private void setGraphName(String name) {
            this.graphName = "Гістограма розподілення " + name + " за розмірами";
        }

        private void setDataset(List<String> data, double max) {
            double step = max / data.size();
            XYSeries series = new XYSeries("");
            for (int i = 0; i < data.size() - 1; i++) {
                double y;
                try { y = Double.parseDouble(data.get(i)); }
                catch (NumberFormatException ex) {
                    y = Double.parseDouble(data.get(i + 1)); i++;
                }
                series.add(i * step, y);
            }
            dataset.addSeries(series);
        }

        private void setMarker(String index) {
            ValueMarker marker = new ValueMarker(Double.parseDouble(index));
            marker.setPaint(Color.black);
            marker.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f));
            plot.addDomainMarker(marker, Layer.FOREGROUND);
        }

        private void generateImage() {
            XYAreaRenderer render = new XYAreaRenderer();
            render.setSeriesPaint(0, Color.decode(getColor()));
            render.setSeriesStroke(0, new BasicStroke(2.5f));
            render.setOutline(true);

            plot.setDataset(dataset);
            plot.setRenderer(0, render);

            BufferedImage img = chart.createBufferedImage(450, 200, null);
            imageString = ImageUtils.encodePngToBase64(img);
            dataset.removeAllSeries();
        }

        private void createChart(String name) {
            switch (name) {
                case "WBC" -> { setGraphName("лейкоцитів"); setColor("#FFF5DD"); }
                case "WBC-LYM-MONO" -> { setGraphName("лейкоцитів \n (лімфоцитів та моноцитів) "); setColor("#FFF5DD"); }
                case "WBC-MONO-POLY" -> { setGraphName("лейкоцитів \n (одноядерних та поліморфноядерних) "); setColor("#FFF5DD"); }
                case "RBC" -> { setGraphName("еритроцитів"); setColor("#9E021E"); }
                case "PLT" -> { setGraphName("тромбоцитів"); setColor("#315691"); }
                default   -> { setGraphName(name); setColor("#000000"); }
            }
            chart = ChartFactory.createXYLineChart(
                    getGraphName(), null, null, null,
                    PlotOrientation.VERTICAL, false, false, false);
            plot = chart.getXYPlot();

            /* базове оформлення */
            chart.setBackgroundPaint(Color.white);
            TextTitle t = chart.getTitle();
            t.setHorizontalAlignment(HorizontalAlignment.CENTER);
            t.setPaint(Color.black);
            t.setFont(new Font("Comic Sans MS", Font.ITALIC, 16));
            plot.setBackgroundPaint(new Color(232, 232, 232));
            plot.setDomainGridlinePaint(Color.gray);
            plot.setRangeGridlinePaint(Color.gray);
            plot.setAxisOffset(new RectangleInsets(1.0, 1.0, 1.0, 1.0));

            ValueAxis axis = plot.getDomainAxis();
            axis.setAxisLineVisible(false);
            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setAxisLineVisible(false);
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        }
    }
}