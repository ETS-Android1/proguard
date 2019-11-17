/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
 */
package proguard.classfile.kotlin.initialize;

import proguard.classfile.*;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitors.*;
import proguard.classfile.util.*;

/**
 * This class initializes the default implementions class of interfaces.
 *
 * The class will be an inner class and have a name like MyInterface$DefaultImpls.
 *
 * Non-abstract interface methods will have a reference initialized to the
 * default implementation method and class.
 */
public class KotlinDefaultImplsInitializer
implements   KotlinMetadataVisitor
{
    private final ClassPool                       programClassPool;
    private final WarningPrinter                  warningPrinter;
    private final MyKotlinDefaultImplsInitializer kotlinDefaultMethodInitializer = new MyKotlinDefaultImplsInitializer();

    public KotlinDefaultImplsInitializer(ClassPool programClassPool, WarningPrinter warningPrinter)
    {
        this.programClassPool = programClassPool;
        this.warningPrinter   = warningPrinter;
    }

    // Implementations for KotlinMetadataVisitor.

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

    @Override
    public void visitKotlinClassMetadata(Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata)
    {
        if (kotlinClassKindMetadata.flags.isInterface)
        {
            kotlinDefaultMethodInitializer.defaultImplsClass =
                programClassPool.getClass(
                    kotlinClassKindMetadata.referencedClass.getName() +
                    KotlinConstants.DEFAULT_IMPLEMENTATIONS_SUFFIX
                );

            kotlinClassKindMetadata.functionsAccept(clazz, kotlinDefaultMethodInitializer);

            kotlinClassKindMetadata.referencedDefaultImplsClass = kotlinDefaultMethodInitializer.defaultImplsClass;
        }
    }

    // Initializer implementation class.

    private static class MyKotlinDefaultImplsInitializer
    implements KotlinFunctionVisitor
    {
        private Clazz defaultImplsClass;

        private static final MemberFinder strictMemberFinder = new MemberFinder(false);

        @Override
        public void visitAnyFunction(Clazz                  clazz,
                                     KotlinMetadata         kotlinMetadata,
                                     KotlinFunctionMetadata kotlinFunctionMetadata) {}

        @Override
        public void visitFunction(Clazz                              clazz,
                                  KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                  KotlinFunctionMetadata             kotlinFunctionMetadata)
        {
            if (defaultImplsClass != null &&
                !kotlinFunctionMetadata.flags.modality.isAbstract)
            {
                kotlinFunctionMetadata.referencedDefaultImplementationMethod =
                    strictMemberFinder.findMethod(
                        defaultImplsClass,
                        kotlinFunctionMetadata.jvmSignature.getName(),
                        getDescriptor(kotlinDeclarationContainerMetadata, kotlinFunctionMetadata)
                    );

                if (kotlinFunctionMetadata.referencedDefaultImplementationMethod != null)
                {
                    kotlinFunctionMetadata.referencedDefaultImplementationMethodClass = defaultImplsClass;
                }
            }
        }
    }

    // Small helper methods.

    /**
     * Default implementation methods are static.
     *
     * The first parameter an instance of the interface.
     *
     * @param kotlinDeclarationContainerMetadata where the function is declared.
     * @param kotlinFunctionMetadata a kotlin function
     * @return the descriptor for the default implementation method.
     */
    private static String getDescriptor(KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                        KotlinFunctionMetadata             kotlinFunctionMetadata)
    {
        return kotlinFunctionMetadata.jvmSignature.getDesc()
            .replace("(", "(L" + kotlinDeclarationContainerMetadata.ownerClassName + ";");
    }
}