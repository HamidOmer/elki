package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.selection;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.RangeSelection;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperCube;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.P2DVisualization;

/**
 * Visualizer for generating an SVG-Element containing a cube as marker
 * representing the selected range for each dimension
 * 
 * @author Heidi Kolb
 * 
 * @apiviz.has SelectionResult oneway - - visualizes
 * @apiviz.has RangeSelection oneway - - visualizes
 * @apiviz.uses SVGHyperCube
 */
public class SelectionCubeVisualization extends P2DVisualization {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Selection Range";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String MARKER = "selectionCubeMarker";

  /**
   * CSS class for the filled cube
   */
  public static final String CSS_CUBE = "selectionCube";

  /**
   * CSS class for the cube frame
   */
  public static final String CSS_CUBEFRAME = "selectionCubeFrame";

  /**
   * Fill parameter.
   */
  protected boolean nofill = false;

  public SelectionCubeVisualization(VisualizationTask task, boolean nofill) {
    super(task);
    this.nofill = nofill;
    addCSSClasses(svgp);
    context.addResultListener(this);
    incrementalRedraw();
  }

  @Override
  public void destroy() {
    context.removeResultListener(this);
    super.destroy();
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    final StyleLibrary style = context.getStyleLibrary();
    // Class for the cube
    if(!svgp.getCSSClassManager().contains(CSS_CUBE)) {
      CSSClass cls = new CSSClass(this, CSS_CUBE);
      cls.setStatement(SVGConstants.CSS_STROKE_VALUE, style.getColor(StyleLibrary.SELECTION));
      cls.setStatement(SVGConstants.CSS_STROKE_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT));
      cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      if(nofill) {
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      }
      else {
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getColor(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
      }
      svgp.addCSSClassOrLogError(cls);
    }
    // Class for the cube frame
    if(!svgp.getCSSClassManager().contains(CSS_CUBEFRAME)) {
      CSSClass cls = new CSSClass(this, CSS_CUBEFRAME);
      cls.setStatement(SVGConstants.CSS_STROKE_VALUE, style.getColor(StyleLibrary.SELECTION));
      cls.setStatement(SVGConstants.CSS_STROKE_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.SELECTION));

      svgp.addCSSClassOrLogError(cls);
    }
  }

  /**
   * Generates a cube and a frame depending on the selection stored in the
   * context
   * 
   * @param svgp The plot
   * @param proj The projection
   */
  private void setSVGRect(SVGPlot svgp, Projection2D proj) {
    DBIDSelection selContext = context.getSelection();
    if(selContext instanceof RangeSelection) {
      DoubleDoublePair[] ranges = ((RangeSelection) selContext).getRanges();
      int dim = DatabaseUtil.dimensionality(rel);

      double[] min = new double[dim];
      double[] max = new double[dim];
      for(int d = 0; d < dim; d++) {
        if(ranges != null && ranges[d] != null) {
          min[d] = ranges[d].first;
          max[d] = ranges[d].second;
        }
        else {
          min[d] = proj.getScale(d).getMin();
          max[d] = proj.getScale(d).getMax();
        }
      }
      if(nofill) {
        Element r = SVGHyperCube.drawFrame(svgp, proj, min, max);
        SVGUtil.setCSSClass(r, CSS_CUBEFRAME);
        layer.appendChild(r);
      }
      else {
        Element r = SVGHyperCube.drawFilled(svgp, CSS_CUBE, proj, min, max);
        layer.appendChild(r);
      }

    }
  }

  @Override
  protected void redraw() {
    DBIDSelection selContext = context.getSelection();
    if(selContext != null && selContext instanceof RangeSelection) {
      setSVGRect(svgp, proj);
    }
  }

  /**
   * Factory for visualizers to generate an SVG-Element containing a cube as
   * marker representing the selected range for each dimension
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses SelectionCubeVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Flag for half-transparent filling of selection cubes.
     * 
     * <p>
     * Key: {@code -selectionrange.nofill}
     * </p>
     */
    public static final OptionID NOFILL_ID = OptionID.getOrCreateOptionID("selectionrange.nofill", "Use wireframe style for selection ranges.");

    /**
     * Fill parameter.
     */
    protected boolean nofill = false;

    /**
     * Constructor.
     * 
     * @param nofill
     */
    public Factory(boolean nofill) {
      super();
      this.nofill = nofill;
      thumbmask |= ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_SELECTION;
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new SelectionCubeVisualization(task, nofill);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      final ArrayList<SelectionResult> selectionResults = ResultUtil.filterResults(result, SelectionResult.class);
      for(SelectionResult selres : selectionResults) {
        IterableIterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ScatterPlotProjector.class);
        for(ScatterPlotProjector<?> p : ps) {
          final VisualizationTask task = new VisualizationTask(NAME, selres, p.getRelation(), this);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA - 2);
          baseResult.getHierarchy().add(selres, task);
          baseResult.getHierarchy().add(p, task);
        }
      }
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      protected boolean nofill;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        Flag nofillF = new Flag(NOFILL_ID);
        if(config.grab(nofillF)) {
          nofill = nofillF.getValue();
        }
      }

      @Override
      protected Factory makeInstance() {
        return new Factory(nofill);
      }
    }
  }
}