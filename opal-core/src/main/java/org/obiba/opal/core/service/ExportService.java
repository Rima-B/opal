/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.core.service;

import java.io.File;
import java.util.List;

import org.obiba.magma.NoSuchDatasourceException;

/**
 * Service for export operations. Export allows Magma tables to be copied to an existing Datasource or to an Excel file.
 */
public interface ExportService {

  /**
   * Export tables to an existing Datasource.
   * @param fromTableNames tables to export.
   * @param destinationDatasourceName tables will be copied to this existing Datasource.
   * @throws NoSuchDatasourceException if the destinationDatasourceName does not exist.
   */
  public void exportTablesToDatasource(List<String> fromTableNames, String destinationDatasourceName);

  /**
   * Export tables to an Excel file.
   * @param fromTableNames tables to export.
   * @param destinationExcelFilename tables will be copied to this Excel file.
   */
  public void exportTablesToExcelFile(List<String> fromTableNames, File destinationExcelFile);

}
