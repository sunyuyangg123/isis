/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.viewer.wicket.ui.components.scalars;

import java.util.List;

import com.google.common.collect.Lists;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponentLabel;
import org.apache.wicket.markup.html.form.LabeledWebMarkupContainer;
import org.apache.wicket.markup.html.panel.ComponentFeedbackPanel;
import org.apache.wicket.model.Model;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facets.members.cssclass.CssClassFacet;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionParameter;
import org.apache.isis.viewer.wicket.model.links.LinkAndLabel;
import org.apache.isis.viewer.wicket.model.mementos.ActionParameterMemento;
import org.apache.isis.viewer.wicket.model.mementos.ObjectAdapterMemento;
import org.apache.isis.viewer.wicket.model.models.EntityModel;
import org.apache.isis.viewer.wicket.model.models.EntityModel.RenderingHint;
import org.apache.isis.viewer.wicket.model.models.ScalarModel;
import org.apache.isis.viewer.wicket.ui.components.additionallinks.AdditionalLinksPanel;
import org.apache.isis.viewer.wicket.ui.components.additionallinks.EntityActionUtil;
import org.apache.isis.viewer.wicket.ui.components.scalars.ScalarPanelAbstract.Rendering;
import org.apache.isis.viewer.wicket.ui.components.scalars.TextFieldValueModel.ScalarModelProvider;
import org.apache.isis.viewer.wicket.ui.panels.PanelAbstract;
import org.apache.isis.viewer.wicket.ui.util.Components;
import org.apache.isis.viewer.wicket.ui.util.CssClassAppender;

/**
 * Adapter for {@link PanelAbstract panel}s that use a {@link ScalarModel} as
 * their backing model.
 * 
 * <p>
 * Supports the concept of being {@link Rendering#COMPACT} (eg within a table) or
 * {@link Rendering#REGULAR regular} (eg within a form).
 */
public abstract class ScalarPanelAbstract extends PanelAbstract<ScalarModel> implements ScalarModelProvider {

    private static final long serialVersionUID = 1L;
    
    private static final String ID_ADDITIONAL_LINKS = "additionalLinks";
    private static final String ID_FEEDBACK = "feedback";

    public enum Rendering {
        /**
         * Does not show labels, eg for use in tables
         */
        COMPACT {
            @Override
            public String getLabelCaption(final LabeledWebMarkupContainer labeledContainer) {
                return "";
            }

            @Override
            public void buildGui(final ScalarPanelAbstract panel) {
                panel.getComponentForRegular().setVisible(false);
            }

            @Override
            public Where getWhere() {
                return Where.PARENTED_TABLES;
            }
        },
        /**
         * Does show labels, eg for use in forms.
         */
        REGULAR {
            @Override
            public String getLabelCaption(final LabeledWebMarkupContainer labeledContainer) {
                return labeledContainer.getLabel().getObject();
            }

            @Override
            public void buildGui(final ScalarPanelAbstract panel) {
                panel.getLabelForCompact().setVisible(false);
            }

            @Override
            public Where getWhere() {
                return Where.OBJECT_FORMS;
            }
        };

        public abstract String getLabelCaption(LabeledWebMarkupContainer labeledContainer);

        public abstract void buildGui(ScalarPanelAbstract panel);

        public abstract Where getWhere();

        private static Rendering renderingFor(RenderingHint renderingHint) {
            return renderingHint.isInTable()? Rendering.COMPACT: Rendering.REGULAR;
        }
    }

    protected Component componentIfCompact;
    private Component componentIfRegular;
    protected final ScalarModel scalarModel;


    public ScalarPanelAbstract(final String id, final ScalarModel scalarModel) {
        super(id, scalarModel);
        this.scalarModel = scalarModel;
    }

    protected Rendering getRendering() {
        return Rendering.renderingFor(getModel().getRenderingHint());
    }

    protected Component getLabelForCompact() {
        return componentIfCompact;
    }

    public Component getComponentForRegular() {
        return componentIfRegular;
    }

    @Override
    protected void onBeforeRender() {
        if (!hasBeenRendered()) {
            buildGui();
        }
        final ScalarModel scalarModel = getModel();
        if (scalarModel.isViewMode()) {
            onBeforeRenderWhenViewMode();
        } else {
            final String disableReasonIfAny = scalarModel.disable(getRendering().getWhere());
            if (disableReasonIfAny != null) {
                onBeforeRenderWhenDisabled(disableReasonIfAny);
            } else {
                onBeforeRenderWhenEnabled();
            }
        }
        super.onBeforeRender();
    }

    /**
     * Builds GUI lazily prior to first render.
     * 
     * <p>
     * This design allows the panel to be configured first, using
     * {@link #setFormat(Rendering)}.
     * 
     * @see #onBeforeRender()
     * @see #setFormat(Rendering)
     */
    private void buildGui() {
        
        // REVIEW: this is nasty, both write to the same entityLink field
        // even though only one is used
        componentIfCompact = addComponentForCompact();
        componentIfRegular = addComponentForRegular();
        
        getRendering().buildGui(this);
        addCssForMetaModel();
        
        if(!subscribers.isEmpty()) {
            addFormComponentBehavior(new AjaxFormComponentUpdatingBehavior("onchange"){

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    for (ScalarModelSubscriber subscriber : subscribers) {
                        subscriber.onUpdate(target, ScalarPanelAbstract.this);
                    }
                }
            });
        }
    }


    /**
     * Mandatory hook.
     */
    protected abstract void addFormComponentBehavior(Behavior behavior);

    private void addCssForMetaModel() {
        final String cssForMetaModel = getModel().getLongName();
        if (cssForMetaModel != null) {
            add(new AttributeAppender("class", Model.of(cssForMetaModel), " "));
        }

        ScalarModel model = getModel();
        final CssClassFacet facet = model.getFacet(CssClassFacet.class);
        if(facet != null) {
              add(new CssClassAppender(facet.value()));
        }
    }

    /**
     * Mandatory hook method to build the component to render the model when in
     * {@link Rendering#REGULAR regular} format.
     */
    protected abstract FormComponentLabel addComponentForRegular();

    protected abstract Component addComponentForCompact();

    protected void addFeedbackTo(MarkupContainer markupContainer, Component component) {
        markupContainer.addOrReplace(new ComponentFeedbackPanel(ID_FEEDBACK, component));
    }
    
    protected void addAdditionalLinksTo(final FormComponentLabel labelIfRegular) {
        final List<LinkAndLabel> entityActions;
        if(scalarModel.getKind() == ScalarModel.Kind.PROPERTY) {
            final ObjectAdapterMemento parentMemento = scalarModel.getParentObjectAdapterMemento();
            final EntityModel parentEntityModel = new EntityModel(parentMemento);
            entityActions = EntityActionUtil.entityActions(parentEntityModel, scalarModel.getPropertyMemento().getProperty());
        } else {
            entityActions = null;
        }
        addAdditionalLinks(labelIfRegular, entityActions);
    }

    private void addAdditionalLinks(MarkupContainer markupContainer, List<LinkAndLabel> links) {
        if(links == null || links.isEmpty()) {
            Components.permanentlyHide(markupContainer, ID_ADDITIONAL_LINKS);
            return;
        }
        links = Lists.newArrayList(links); // copy, to serialize any lazy evaluation
        
        final WebMarkupContainer views = new AdditionalLinksPanel(ID_ADDITIONAL_LINKS, links);
        markupContainer.addOrReplace(views);
    }

    /**
     * Optional hook.
     */
    protected void onBeforeRenderWhenViewMode() {
    }

    /**
     * Optional hook.
     */
    protected void onBeforeRenderWhenDisabled(final String disableReason) {
    }

    /**
     * Optional hook.
     */
    protected void onBeforeRenderWhenEnabled() {
    }

    // //////////////////////////////////////

    private final List<ScalarModelSubscriber> subscribers = Lists.newArrayList();

    public void notifyOnChange(final ScalarModelSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    // //////////////////////////////////////

    /**
     * Optional hook method
     */
    public void updateChoices(ObjectAdapter[] pendingArguments) {
    }


}
