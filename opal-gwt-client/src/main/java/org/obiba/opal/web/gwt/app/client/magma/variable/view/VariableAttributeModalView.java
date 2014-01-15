/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.web.gwt.app.client.magma.variable.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.gwt.app.client.magma.variable.presenter.VariableAttributeModalPresenter;
import org.obiba.opal.web.gwt.app.client.magma.variable.presenter.VariableAttributeModalUiHandlers;
import org.obiba.opal.web.gwt.app.client.ui.LocalizedEditableText;
import org.obiba.opal.web.gwt.app.client.ui.Modal;
import org.obiba.opal.web.gwt.app.client.ui.ModalPopupViewWithUiHandlers;
import org.obiba.opal.web.gwt.app.client.ui.SuggestListBox;
import org.obiba.opal.web.model.client.magma.AttributeDto;
import org.obiba.opal.web.model.client.magma.VariableDto;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Typeahead;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.TakesValue;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import static org.obiba.opal.web.gwt.app.client.magma.variable.presenter.VariableAttributeModalPresenter.Display;

/**
 *
 */
public class VariableAttributeModalView extends ModalPopupViewWithUiHandlers<VariableAttributeModalUiHandlers>
    implements Display {

  private final Translations translations;

  interface Binder extends UiBinder<Widget, VariableAttributeModalView> {}

  @UiField
  Modal modal;

  @UiField
  Button saveButton;

  @UiField
  Button cancelButton;

  @UiField
  ControlGroup nameGroup;

  @UiField
  Typeahead namespaceTypeahead;

  @UiField
  TextBox namespace;

  @UiField
  TextBox name;

  @UiField
  ControlGroup valuesGroup;

  @UiField
  FlowPanel valuesPanel;

  @Inject
  public VariableAttributeModalView(Binder uiBinder, EventBus eventBus, Translations translations) {
    super(eventBus);
    this.translations = translations;
    initWidget(uiBinder.createAndBindUi(this));
    modal.setTitle(translations.addAttribute());
  }

  @UiHandler("saveButton")
  public void onSave(ClickEvent event) {
    getUiHandlers().save();
  }

  @UiHandler("cancelButton")
  public void onCancel(ClickEvent event) {
    getUiHandlers().cancel();
  }

  @Override
  public HasText getName() {
    return name;
  }

  @Override
  public TextBox getNamespaceSuggestBox() {
    return namespace;
  }

  @Override
  public void setNamespaceSuggestions(VariableDto variableDto) {
    MultiWordSuggestOracle oracle = (MultiWordSuggestOracle) namespaceTypeahead.getSuggestOracle();
    oracle.clear();
    JsArray<AttributeDto> attributesArray = variableDto.getAttributesArray();

    if (attributesArray != null) {
      for(AttributeDto attributeDto : JsArrays.toList(attributesArray)) {
        if (attributeDto.hasNamespace()) oracle.add(attributeDto.getNamespace());
      }
    }
  }

  @Override
  public void hideDialog() {
    modal.hide();
  }

  @Override
  public void setDialogMode(VariableAttributeModalPresenter.Mode mode) {
    if(mode == VariableAttributeModalPresenter.Mode.UPDATE_MULTIPLE) {
      valuesGroup.setVisible(false);
      nameGroup.setVisible(false);

      modal.setTitle(translations.editAttributes());
    } else if(mode == VariableAttributeModalPresenter.Mode.UPDATE_SINGLE) {
      modal.setTitle(translations.editAttribute());
    }
  }

  @Override
  public TakesValue<List<LocalizedEditableText>> getLocalizedValues() {
    return new TakesValue<List<LocalizedEditableText>>() {
      @Override
      public void setValue(List<LocalizedEditableText> values) {
        if(values != null) {
          for(LocalizedEditableText value : values) {
            valuesPanel.add(value);
          }
        }
      }

      @Override
      public List<LocalizedEditableText> getValue() {
        List<LocalizedEditableText> selected = new ArrayList<LocalizedEditableText>();
        for(int i = 0; i < valuesPanel.getWidgetCount(); i++) {
          selected.add((LocalizedEditableText) valuesPanel.getWidget(i));
        }

        return selected;
      }
    };
  }

  @Override
  public void showError(@Nullable FormField formField, String message) {
    ControlGroup group = null;
    if(formField != null) {
      switch(formField) {
        case NAME:
          group = nameGroup;
          break;
      }
    }
    if(group == null) {
      modal.addAlert(message, AlertType.ERROR);
    } else {
      modal.addAlert(message, AlertType.ERROR, group);
    }
  }

  @Override
  public void clearErrors() {
    modal.clearAlert();
  }
}
