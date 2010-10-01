/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.wizard.createdatasource.presenter;

import net.customware.gwt.presenter.client.Presenter;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;

import org.obiba.opal.web.model.client.magma.DatasourceFactoryDto;

/**
 *
 */
public interface DatasourceFormPresenter extends Presenter {

  public boolean isForType(String type);

  public WidgetDisplay getDisplay();

  public DatasourceFactoryDto getDatasourceFactory();

  /**
   * True if the form data in the form are valid. Responsible for displaying the appropriate error message.
   * @return
   */
  public boolean validate();

  public interface Display extends WidgetDisplay {

  }

}
