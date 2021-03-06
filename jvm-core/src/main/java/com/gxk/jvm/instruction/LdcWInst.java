package com.gxk.jvm.instruction;

import com.gxk.jvm.rtda.Frame;
import com.gxk.jvm.rtda.Slot;
import com.gxk.jvm.rtda.MetaSpace;
import com.gxk.jvm.rtda.UnionSlot;
import com.gxk.jvm.rtda.heap.KArray;
import com.gxk.jvm.rtda.heap.KClass;
import com.gxk.jvm.rtda.heap.KField;
import com.gxk.jvm.rtda.heap.KObject;

public class LdcWInst implements Instruction {

  public final String descriptor;
  public final Object val;

  @Override
  public int offset() {
    return 3;
  }

  public LdcWInst(String descriptor, Object val) {
    this.descriptor = descriptor;
    this.val = val;
  }

  @Override
  public void execute(Frame frame) {
    switch (descriptor) {
      case "I":
        frame.pushInt(((Integer) val));
        break;
      case "F":
        frame.pushFloat(((float) val));
        break;
      case "Ljava/lang/String":
        KClass klass = MetaSpace.findClass("java/lang/String");
        if (klass == null) {
          klass = frame.method.clazz.classLoader.loadClass("java/lang/String");
        }
        if (!klass.isStaticInit()) {
          Frame newFrame = new Frame(klass.getClinitMethod(), frame.thread);
          klass.setStaticInit(1);
          KClass finalKlass = klass;
          newFrame.setOnPop(() -> finalKlass.setStaticInit(2));
          frame.thread.pushFrame(newFrame);

          frame.nextPc = frame.thread.getPc();
          return;
        }
        KObject object = klass.newObject();
        KField field = object.getField("value", "[C");
        KClass arrClazz = new KClass(1, "[C", frame.method.clazz.classLoader, null);

        char[] chars = val.toString().toCharArray();
        char[] characters = new char[chars.length];
        System.arraycopy(chars, 0, characters, 0, chars.length);
        KArray arr = new KArray(arrClazz, characters, characters.length);
        field.val = UnionSlot.of(arr);
        frame.pushRef(object);
        break;
      default:
        frame.pushRef((KObject) val);
        break;
    }
  }

  @Override
  public String format() {
    return "ldcw " + descriptor + " " + val;
  }
}
