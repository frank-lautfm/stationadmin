/**
 * 
 */
package de.stationadmin.gui.util;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.beans.BeanAdapter;
import com.jgoodies.binding.value.ValueModel;

/**
 * @author Frank
 *
 */
public class NonObservingPresentationModel<B> extends PresentationModel<B> {
  private static final long serialVersionUID = -8899881162426794500L;

  /**
   * @param bean
   * @param triggerChannel
   */
  public NonObservingPresentationModel(B bean, ValueModel triggerChannel) {
    super(bean, triggerChannel);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param bean
   */
  public NonObservingPresentationModel(B bean) {
    super(bean);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param beanChannel
   * @param triggerChannel
   */
  public NonObservingPresentationModel(ValueModel beanChannel, ValueModel triggerChannel) {
    super(beanChannel, triggerChannel);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param beanChannel
   */
  public NonObservingPresentationModel(ValueModel beanChannel) {
    super(beanChannel);
    // TODO Auto-generated constructor stub
  }

  /* (non-Javadoc)
   * @see com.jgoodies.binding.PresentationModel#createBeanAdapter(com.jgoodies.binding.value.ValueModel)
   */
  @Override
  protected BeanAdapter<B> createBeanAdapter(ValueModel beanChannel) {
    return new BeanAdapter<B>(beanChannel, false);
  }

}
