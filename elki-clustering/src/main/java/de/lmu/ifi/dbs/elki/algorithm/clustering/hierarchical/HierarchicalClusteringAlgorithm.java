/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.database.Database;

/**
 * Interface for hierarchical clustering algorithms.
 * <p>
 * This interface allows the algorithms to be used by, e.g.,
 * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.CutDendrogramByNumberOfClusters}
 * and
 * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.CutDendrogramByHeight}.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @apiviz.has PointerHierarchyRepresentationResult
 */
public interface HierarchicalClusteringAlgorithm extends Algorithm {
  @Override
  PointerHierarchyRepresentationResult run(Database db);
}
