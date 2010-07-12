/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.widgets.view;

import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.widgets.presenter.ConfirmationPresenter.Display;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 */
public class ConfirmationView extends DialogBox implements Display {
  //
  // Constants
  //

  private static final String DIALOG_HEIGHT = "20em";

  private static final String DIALOG_WIDTH = "32em";

  //
  // Static Variables
  //

  private static ConfirmationViewUiBinder uiBinder = GWT.create(ConfirmationViewUiBinder.class);

  private static Translations translations = GWT.create(Translations.class);

  //
  // Instance Variables
  //

  @UiField
  HTML message;

  @UiField
  Button yesButton;

  @UiField
  Button noButton;

  //
  // Constructors
  //

  public ConfirmationView() {
    setHeight(DIALOG_HEIGHT);
    setWidth(DIALOG_WIDTH);

    DockLayoutPanel content = uiBinder.createAndBindUi(this);
    content.setHeight(DIALOG_HEIGHT);
    content.setWidth(DIALOG_WIDTH);
    add(content);
  }

  //
  // ConfirmationPresenter.Display Methods
  //

  public void showDialog() {
    center();
    show();
  }

  public void hideDialog() {
    hide();
  }

  public void setConfirmationTitle(String title) {
    setText(translations.confirmationTitleMap().get(title));
  }

  public void setConfirmationMessage(String message) {
    this.message.setHTML(translations.confirmationMessageMap().get(message));
  }

  @Override
  public HandlerRegistration addYesButtonHandler(ClickHandler clickHandler) {
    return yesButton.addClickHandler(clickHandler);
  }

  @Override
  public HandlerRegistration addNoButtonHandler(ClickHandler clickHandler) {
    return noButton.addClickHandler(clickHandler);
  }

  @Override
  public void startProcessing() {
  }

  @Override
  public void stopProcessing() {
  }

  @Override
  public Widget asWidget() {
    return this;
  }

  //
  // Inner Classes / Interfaces
  //

  @UiTemplate("ConfirmationView.ui.xml")
  interface ConfirmationViewUiBinder extends UiBinder<DockLayoutPanel, ConfirmationView> {
  }
}