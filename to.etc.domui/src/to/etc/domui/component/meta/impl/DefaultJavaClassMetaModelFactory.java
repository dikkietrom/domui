package to.etc.domui.component.meta.impl;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import javax.annotation.*;

import to.etc.domui.component.meta.*;
import to.etc.domui.converter.*;
import to.etc.domui.trouble.*;
import to.etc.domui.util.*;
import to.etc.util.*;

/**
 * A default, base implementation of a MetaModel layer. This tries to discover metadata by using
 * base property information plus Hibernate/JPA annotation data.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jun 16, 2008
 */
public class DefaultJavaClassMetaModelFactory implements IClassMetaModelFactory {
	@Override
	public int accepts(@Nonnull Object theThingy) {
		if(!(theThingy instanceof Class< ? >)) // Only accept Class<?> thingies.
			return -1;
		return 1;
	}

	/**
	 *
	 * @see to.etc.domui.component.meta.IClassMetaModelFactory#createModel(java.lang.Object)
	 */
	@Override
	@Nonnull
	public ClassMetaModel createModel(@Nonnull Object theThingy) {
		Class< ? > clz = (Class< ? >) theThingy;
		DefaultClassMetaModel dmm = new DefaultClassMetaModel(clz);

		decodeClassAnnotations(dmm, clz); // Do class-level annotations
		decodeDomainValues(dmm, clz); // Handle domain for this class (list-of-values)
		decodeProperties(dmm, clz);

		return dmm;
	}

	/**
	 * This obtains all properties from the class and initializes their models.
	 * @param dmm
	 * @param clz
	 */
	protected void decodeProperties(DefaultClassMetaModel cmm, Class< ? > clz) {
		/*
		 * Business as usual: the Introspector does not properly resolve properties when using
		 * invariant returns. We're forced to do something by ourselves. The Introspector does
		 * not return the defined method in the class, but it returns the synthetic proxy generated
		 * by the compiler with the fixed "Object" return type. This means that the return type
		 * would be incorrect, but even worse: the generated method lacks the annotations on
		 * the real method. This caused metadata to be unavailable for classes that implemented
		 * IIdentifyable&gt;Long&gl;.
		 *
		 * BeanInfo bi = Introspector.getBeanInfo(m_metaClass);
		 * PropertyDescriptor[] ar = bi.getPropertyDescriptors();
		 */
		List<PropertyInfo> pilist = ClassUtil.getProperties(clz);

		//-- Create model data from this thingy.
		for(PropertyInfo pd : pilist) {
			if(!pd.getName().equals("class"))
				createPropertyInfo(cmm, pd);
		}
	}

	protected void createPropertyInfo(DefaultClassMetaModel cmm, final PropertyInfo pd) {
		//		System.out.println("Property: " + pd.getName() + ", reader=" + pd.getGetter());
		//		if(pd.getName().equals("id"))
		//			System.out.println("GOTCHA");

		Method rm = pd.getGetter();
		if(rm.getParameterTypes().length != 0)
			return;
		DefaultPropertyMetaModel pm = new DefaultPropertyMetaModel(cmm, pd);
		cmm.addProperty(pm);
		initPropertyModel(cmm, pd, pm);
		if(pm.isPrimaryKey())
			cmm.setPrimaryKey(pm);
	}

	protected void initPropertyModel(DefaultClassMetaModel cmm, PropertyInfo pd, DefaultPropertyMetaModel pmm) {
		pmm.setAccessor(new PropertyAccessor<Object>(pd.getGetter(), pd.getSetter(), pmm));
		if(pd.getSetter() == null) {
			pmm.setReadOnly(YesNoType.YES);
		}

		Annotation[] annar = pd.getGetter().getAnnotations();
		for(Annotation an : annar) {
			String ana = an.annotationType().getName();
			decodePropertyAnnotationByName(cmm, pmm, an, ana);
			decodePropertyAnnotation(cmm, pmm, an);
		}
	}

	@SuppressWarnings({"cast", "unchecked"})
	protected void decodePropertyAnnotation(DefaultClassMetaModel cmm, DefaultPropertyMetaModel pmm, Annotation an) {
		if(an instanceof MetaProperty) {
			//-- Handle meta-assignments.
			MetaProperty mp = (MetaProperty) an;
			if(mp.defaultSortable() != SortableType.UNKNOWN)
				pmm.setSortable(mp.defaultSortable());
			if(mp.length() >= 0)
				pmm.setLength(mp.length());
			if(mp.displaySize() >= 0)
				pmm.setDisplayLength(mp.displaySize());
			if(mp.required() != YesNoType.UNKNOWN)
				pmm.setRequired(mp.required() == YesNoType.YES);
			if(mp.converterClass() != DummyConverter.class)
				pmm.setConverter((IConverter) ConverterRegistry.getConverterInstance((Class) mp.converterClass()));
			if(mp.editpermissions().length != 0)
				pmm.setEditRoles(makeRoleSet(mp.editpermissions()));
			if(mp.viewpermissions().length != 0)
				pmm.setViewRoles(makeRoleSet(mp.viewpermissions()));
			if(mp.temporal() != TemporalPresentationType.UNKNOWN && pmm.getTemporal() == TemporalPresentationType.UNKNOWN)
				pmm.setTemporal(mp.temporal());
			if(mp.numericPresentation() != NumericPresentation.UNKNOWN)
				pmm.setNumericPresentation(mp.numericPresentation());
			if(pmm.getReadOnly() != YesNoType.YES) // Do not override readonlyness from missing write method
				pmm.setReadOnly(mp.readOnly());
			if(mp.componentTypeHint().length() != 0)
				pmm.setComponentTypeHint(mp.componentTypeHint());

			//-- Convert validators.
			List<MetaPropertyValidatorImpl> list = new ArrayList<MetaPropertyValidatorImpl>();
			for(Class< ? extends IValueValidator< ? >> vv : mp.validator()) {
				MetaPropertyValidatorImpl vi = new MetaPropertyValidatorImpl(vv);
				list.add(vi);
			}
			for(MetaValueValidator mvv : mp.parameterizedValidator()) {
				MetaPropertyValidatorImpl vi = new MetaPropertyValidatorImpl(mvv.validator(), mvv.parameters());
				list.add(vi);
			}
			pmm.setValidators(list.toArray(new PropertyMetaValidator[list.size()]));

			//-- Regexp validators.
			if(mp.regexpValidation().length() > 0) {
				try {
					//-- Precompile to make sure it's valid;
					Pattern.compile(mp.regexpValidation());
				} catch(Exception x) {
					throw new MetaModelException(Msgs.BUNDLE, Msgs.MM_BAD_REGEXP, mp.regexpValidation(), this.toString());
				}
				pmm.setRegexpValidator(mp.regexpValidation());
				if(mp.regexpUserString().length() > 0)
					pmm.setRegexpUserString(mp.regexpUserString());
			}
		} else if(an instanceof MetaCombo) {
			MetaCombo c = (MetaCombo) an;
			if(c.dataSet() != UndefinedComboDataSet.class) {
				pmm.setRelationType(PropertyRelationType.UP);
				pmm.setComboDataSet(c.dataSet());
			}
			if(c.labelRenderer() != UndefinedLabelStringRenderer.class) {
				pmm.setRelationType(PropertyRelationType.UP);
				pmm.setComboLabelRenderer(c.labelRenderer());
			}
			if(c.nodeRenderer() != UndefinedLabelStringRenderer.class) {
				pmm.setRelationType(PropertyRelationType.UP);
				pmm.setComboNodeRenderer(c.nodeRenderer());
			}
			if(c.optional() != ComboOptionalType.INHERITED)
				pmm.setRequired(c.optional() == ComboOptionalType.REQUIRED);
			if(c.properties() != null && c.properties().length > 0) {
				pmm.setRelationType(PropertyRelationType.UP);
				pmm.setComboDisplayProperties(DisplayPropertyMetaModel.decode(cmm, c.properties()));
			}
			pmm.setComponentTypeHint(Constants.COMPONENT_COMBO);
		} else if(an instanceof SearchProperty) {
			SearchProperty sp = (SearchProperty) an;
			SearchPropertyMetaModelImpl mm = new SearchPropertyMetaModelImpl(cmm);
			mm.setIgnoreCase(sp.ignoreCase());
			mm.setOrder(sp.order());
			mm.setMinLength(sp.minLength());
			mm.setPropertyName(pmm.getName());
			//			mm.setProperty(this);
			if(((SearchProperty) an).searchType() == SearchPropertyType.SEARCH_FIELD || ((SearchProperty) an).searchType() == SearchPropertyType.BOTH) {
				((DefaultClassMetaModel) cmm).addSearchProperty(mm);
			}
			if(((SearchProperty) an).searchType() == SearchPropertyType.KEYWORD || ((SearchProperty) an).searchType() == SearchPropertyType.BOTH) {
				((DefaultClassMetaModel) cmm).addKeyWordSearchProperty(mm);
			}

		} else if(an instanceof MetaObject) {
			MetaObject o = (MetaObject) an;
			if(o.defaultColumns().length > 0) {
				pmm.setTableDisplayProperties(DisplayPropertyMetaModel.decode(cmm, o.defaultColumns()));
			}
		} else if(an instanceof MetaLookup) {
			MetaLookup c = (MetaLookup) an;
			if(c.nodeRenderer() != UndefinedLabelStringRenderer.class)
				pmm.setLookupFieldRenderer(c.nodeRenderer());
			if(c.properties().length != 0)
				pmm.setLookupFieldDisplayProperties(DisplayPropertyMetaModel.decode(cmm, c.properties()));
			pmm.setComponentTypeHint(Constants.COMPONENT_LOOKUP);
		}
	}

	protected void decodePropertyAnnotationByName(DefaultClassMetaModel cmm, DefaultPropertyMetaModel pmm, Annotation an, String name) {
		if("javax.persistence.Column".equals(name)) {
			decodeJpaColumn(pmm, an);
		} else if("javax.persistence.Id".equals(name)) {
			pmm.setPrimaryKey(true);
			cmm.setPersistentClass(true);
		} else if("javax.persistence.ManyToOne".equals(name)) {
			pmm.setRelationType(PropertyRelationType.UP);

			//-- Decode fields from the annotation.
			try {
				Boolean op = (Boolean) DomUtil.getClassValue(an, "optional");
				pmm.setRequired(!op.booleanValue());
			} catch(Exception x) {
				Trouble.wrapException(x);
			}
		} else if("javax.persistence.Temporal".equals(name)) {
			try {
				Object val = DomUtil.getClassValue(an, "value");
				if(val != null) {
					String s = val.toString();
					if("DATE".equals(s))
						pmm.setTemporal(TemporalPresentationType.DATE);
					else if("TIME".equals(s))
						pmm.setTemporal(TemporalPresentationType.TIME);
					else if("TIMESTAMP".equals(s))
						pmm.setTemporal(TemporalPresentationType.DATETIME);
				}
			} catch(Exception x) {
				Trouble.wrapException(x);
			}
		} else if("javax.persistence.Transient".equals(name)) {
			pmm.setTransient(true);
		} else if("javax.persistence.OneToMany".equals(name)) {
			//-- This must be a list
			if(!Collection.class.isAssignableFrom(pmm.getActualType()))
				throw new IllegalStateException("Invalid property type for DOWN relation of property " + this + ": only List<T> is allowed");
			pmm.setRelationType(PropertyRelationType.DOWN);
		} else if("to.etc.webapp.qsql.QJdbcId".equals(name)) {
			pmm.setPrimaryKey(true);
		}
	}

	/**
	 * Generically decode a JPA javax.persistence.Column annotation.
	 * @param pmm
	 * @param an
	 */
	protected void decodeJpaColumn(DefaultPropertyMetaModel pmm, final Annotation an) {
		try {
			/*
			 * Handle the "length" annotation. As usual, someone with a brain the size of a pea fucked up the standard. The
			 * default value for the length is 255, which is of course a totally reasonable size. This makes it impossible to
			 * see if someone has actually provided a value. This means that in absence of the length the field is fucking
			 * suddenly 255 characters big. To prevent this utter disaster from fucking up the data we only accept this for
			 * STRING fields.
			 */
			Integer iv = (Integer) DomUtil.getClassValue(an, "length");
			pmm.setLength(iv.intValue());
			if(pmm.getLength() == 255) { // Idiot value?
				if(pmm.getActualType() != String.class)
					pmm.setLength(-1);
			}

			Boolean bv = (Boolean) DomUtil.getClassValue(an, "nullable");
			pmm.setRequired(!bv.booleanValue());
			iv = (Integer) DomUtil.getClassValue(an, "precision");
			pmm.setPrecision(iv.intValue());
			iv = (Integer) DomUtil.getClassValue(an, "scale");
			pmm.setScale(iv.intValue());
		} catch(RuntimeException x) {
			throw x;
		} catch(Exception x) {
			throw new WrappedException(x);
		}
	}

	/**
	 * Decode stringset containing xxx+xxx to duparray.
	 * @param s
	 * @return
	 */
	static private String[][] makeRoleSet(final String[] sar) {
		if(sar.length == 0)
			return null;
		String[][] mapset = new String[sar.length][];
		ArrayList<String> al = new ArrayList<String>(10);
		int ix = 0;
		for(String s : sar) {
			StringTokenizer st = new StringTokenizer(s, ";+ \t,");
			al.clear();
			while(st.hasMoreElements()) {
				al.add(st.nextToken());
			}
			mapset[ix] = al.toArray(new String[al.size()]);
		}
		return mapset;
	}

	/**
	 * If this is an enum or the class Boolean define it's domain values.
	 * @param dmm
	 * @param clz
	 */
	protected void decodeDomainValues(DefaultClassMetaModel dmm, Class< ? > clz) {
		//-- If this is an enumerable thingerydoo...
		if(clz == Boolean.class) {
			dmm.setDomainValues(new Object[]{Boolean.FALSE, Boolean.TRUE});
		} else if(Enum.class.isAssignableFrom(clz)) {
			Class<Enum< ? >> ecl = (Class<Enum< ? >>) clz;
			dmm.setDomainValues(ecl.getEnumConstants());
		}
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Annotation handling.								*/
	/*--------------------------------------------------------------*/
	/**
	 * Walk all known class annotations and use them to add class based metadata.
	 */
	protected void decodeClassAnnotations(DefaultClassMetaModel cmm, Class< ? > clz) {
		Annotation[] annar = clz.getAnnotations(); // All class-level thingerydoos
		for(Annotation an : annar) {
			String ana = an.annotationType().getName(); // Get the annotation's name
			decodeAnnotationByName(cmm, an, ana); // Decode by name literal
			decodeAnnotation(cmm, an); // Decode well-known annotations
		}
	}

	/**
	 * Can be overridden to decode user-specific annotations. Currently only decodes the javax.persistence.Table annotation.
	 * @param an
	 * @param name
	 */
	protected void decodeAnnotationByName(@Nonnull final DefaultClassMetaModel cmm, @Nonnull final Annotation an, @Nonnull final String name) {
		if("javax.persistence.Table".equals(name)) {
			//-- Decode fields from the annotation.
			try {
				String tablename = (String) DomUtil.getClassValue(an, "name");
				String tableschema = (String) DomUtil.getClassValue(an, "schema");
				if(tablename != null) {
					if(tableschema != null)
						tablename = tableschema + "." + tablename;
					cmm.setTableName(tablename);
				}
			} catch(Exception x) {
				Trouble.wrapException(x);
			}
		}
	}

	/**
	 * Decodes all DomUI annotations.
	 * @param an
	 */
	protected void decodeAnnotation(final DefaultClassMetaModel cmm, final Annotation an) {
		if(an instanceof MetaCombo) {
			MetaCombo c = (MetaCombo) an;
			if(c.dataSet() != UndefinedComboDataSet.class)
				cmm.setComboDataSet(c.dataSet());
			if(c.labelRenderer() != UndefinedLabelStringRenderer.class)
				cmm.setComboLabelRenderer(c.labelRenderer());
			if(c.nodeRenderer() != UndefinedLabelStringRenderer.class)
				cmm.setComboNodeRenderer(c.nodeRenderer());
			if(c.optional() != ComboOptionalType.INHERITED)
				cmm.setComboOptional(c.optional());
			if(c.properties() != null && c.properties().length > 0) {
				cmm.setComboDisplayProperties(DisplayPropertyMetaModel.decode(cmm, c.properties()));
			}
			cmm.setComponentTypeHint(Constants.COMPONENT_COMBO);
		} else if(an instanceof MetaLookup) {
			MetaLookup c = (MetaLookup) an;
			if(c.nodeRenderer() != UndefinedLabelStringRenderer.class)
				cmm.setLookupFieldRenderer(c.nodeRenderer());
			if(c.properties().length != 0)
				cmm.setLookupFieldDisplayProperties(DisplayPropertyMetaModel.decode(cmm, c.properties()));
			cmm.setComponentTypeHint(Constants.COMPONENT_LOOKUP);
		} else if(an instanceof MetaObject) {
			MetaObject mo = (MetaObject) an;
			if(mo.defaultColumns().length > 0) {
				cmm.setTableDisplayProperties(DisplayPropertyMetaModel.decode(cmm, mo.defaultColumns()));
			}
			if(!mo.defaultSortColumn().equals(Constants.NONE))
				cmm.setDefaultSortProperty(mo.defaultSortColumn());
			cmm.setDefaultSortDirection(mo.defaultSortOrder());
		} else if(an instanceof MetaSearch) {
			MetaSearch ms = (MetaSearch) an;
			int index = 0;
			for(MetaSearchItem msi : ms.value()) {
				index++;
				SearchPropertyMetaModelImpl mm = new SearchPropertyMetaModelImpl(cmm);
				mm.setIgnoreCase(msi.ignoreCase());
				mm.setOrder(msi.order() == -1 ? index : msi.order());
				mm.setMinLength(msi.minLength());
				mm.setPropertyName(msi.name().length() == 0 ? null : msi.name());
				mm.setLookupLabelKey(msi.lookupLabelKey().length() == 0 ? null : msi.lookupLabelKey());
				mm.setLookupHintKey(msi.lookupHintKey().length() == 0 ? null : msi.lookupHintKey());

				//FIXME NEED TO ADD HERE ACCORDING TO TYPE.
				//				if(msi. == SearchPropertyType.SEARCH_FIELD) {
				//					((DefaultClassMetaModel) getClassModel()).addSearchProperty(mm);
				//				}
				//				if(searchType == SearchPropertyType.KEYWORD) {
				//					((DefaultClassMetaModel) getClassModel()).addKeyWordSearchProperty(mm);
				//				}
				cmm.addSearchProperty(mm);
			}
		}
	}

}