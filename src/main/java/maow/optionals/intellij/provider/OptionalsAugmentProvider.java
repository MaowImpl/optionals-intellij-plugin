package maow.optionals.intellij.provider;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.impl.light.LightParameterListBuilder;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static maow.optionals.intellij.Names.OPTIONAL;


public final class OptionalsAugmentProvider extends PsiAugmentProvider {
    @NotNull
    @SuppressWarnings("deprecation")
    @Override
    protected <Psi extends PsiElement> List<Psi> getAugments(@NotNull PsiElement element, @NotNull Class<Psi> type) {
        return getAugments(element, type, null);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    protected <Psi extends PsiElement> List<Psi> getAugments(@NotNull PsiElement element, @NotNull Class<Psi> type, @Nullable String nameHint) {
        if (!(element instanceof PsiExtensibleClass) || type != PsiMethod.class) {
            return Collections.emptyList();
        }

        final PsiExtensibleClass clazz = (PsiExtensibleClass) element;

        final List<Psi> results = new ArrayList<>();

        for (PsiMethod method : clazz.getOwnMethods()) {
            final PsiParameter[] parameters = method.getParameterList().getParameters();
            final int total = (int) Arrays
                    .stream(parameters)
                    .filter(parameter -> parameter.hasAnnotation(OPTIONAL))
                    .count();
            if (total != 0) {
                final List<Psi> overloads = (List<Psi>)
                        getOverloadMethods(clazz, method, total);
                results.addAll(overloads);
            }
        }

        return results;
    }

    private List<LightMethodBuilder> getOverloadMethods(PsiClass clazz, PsiMethod base, int total) {
        final List<LightMethodBuilder> overloads = new ArrayList<>();
        for (int i = 1; i <= total; i++) {
            final List<PsiParameter> parameters = getOverloadParameters(base, i);
            final LightMethodBuilder overload = getOverloadMethod(clazz, base, parameters);
            overloads.add(overload);
        }
        return overloads;
    }

    private List<PsiParameter> getOverloadParameters(PsiMethod base, int skip) {
        final List<PsiParameter> overloadParams = new ArrayList<>();
        final List<PsiParameter> parameters = getParameters(base);
        int skipped = 0;
        for (PsiParameter parameter : parameters) {
            if (parameter.hasAnnotation(OPTIONAL) && skipped != skip) {
                skipped += 1;
                continue;
            }
            overloadParams.add(parameter);
        }
        Collections.reverse(overloadParams);
        return overloadParams;
    }

    private List<PsiParameter> getParameters(PsiMethod base) {
        final PsiParameter[] parameters = base.getParameterList().getParameters();
        final List<PsiParameter> parameterList = Arrays.asList(parameters);
        Collections.reverse(parameterList);
        return parameterList;
    }

    private LightMethodBuilder getOverloadMethod(PsiClass clazz, PsiMethod base, List<PsiParameter> parameters) {
        final String name = base.getName();
        final PsiType type = base.getReturnType();
        final PsiModifierList mods = base.getModifierList();

        final PsiManager manager = clazz.getContainingFile().getManager();
        final LightParameterListBuilder lightParams = new LightParameterListBuilder(manager, JavaLanguage.INSTANCE);
        parameters.forEach(lightParams::addParameter);

        return new LightMethodBuilder(
                manager,
                JavaLanguage.INSTANCE,
                name,
                lightParams,
                mods
        )
                .setConstructor(base.isConstructor())
                .setMethodReturnType(type)
                .setContainingClass(clazz);
    }
}