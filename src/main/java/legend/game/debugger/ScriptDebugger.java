package legend.game.debugger;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import legend.core.Hardware;
import legend.core.memory.types.IntRef;
import legend.core.memory.types.Pointer;
import legend.game.modding.events.EventListener;
import legend.game.modding.events.EventManager;
import legend.game.modding.events.scripting.ScriptAllocatedEvent;
import legend.game.modding.events.scripting.ScriptDeallocatedEvent;
import legend.game.modding.events.scripting.ScriptTickEvent;
import legend.game.types.ScriptState;

import static legend.game.Scus94491BpeSegment_800b.scriptStatePtrArr_800bc1c0;
import static legend.game.Scus94491BpeSegment_800b.scriptState_800bc0c0;

public class ScriptDebugger extends Application {
  @FXML
  private ComboBox<ListItem> scriptSelector;
  private final ObservableList<ListItem> scripts = FXCollections.observableArrayList(e -> new Observable[] {e.prop});

  @FXML
  private ListView<ListItem> scriptStorage;
  private final ObservableList<ListItem> storage = FXCollections.observableArrayList(e -> new Observable[] {e.prop});

  @FXML
  public TextField stackTop;
  @FXML
  private ListView<ListItem> commandStack;
  private final ObservableList<ListItem> stack = FXCollections.observableArrayList(e -> new Observable[] {e.prop});

  @FXML
  public TextField callback04;
  @FXML
  public TextField callback08;
  @FXML
  public TextField callback10;
  @FXML
  public TextField destructor;
  @FXML
  public TextField filePtr;
  @FXML
  public TextField parentIndex;
  @FXML
  public TextField childIndex;

  private int preselectScript;

  public ScriptDebugger preselectScript(final int preselectScript) {
    this.preselectScript = preselectScript;
    return this;
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final FXMLLoader loader = new FXMLLoader(getClass().getResource("script_debugger.fxml"));
    final Parent root = loader.load();
    final Scene scene = new Scene(root);
    scene.getStylesheets().add(getClass().getResource("script_debugger.css").toExternalForm());

    final ScriptDebugger controller = loader.getController();
    controller.selectScript(this.preselectScript);

    stage.setTitle("Script Debugger");
    stage.setScene(scene);
    stage.show();
  }

  public void initialize() {
    for(int i = 0; i < 0x48; i++) {
      this.scripts.add(new ListItem(this::getScriptName, i));
    }

    this.scriptSelector.setItems(this.scripts);
    this.scriptSelector.setConverter(new StringConverter<>() {
      @Override
      public String toString(final ListItem object) {
        return object != null ? object.getName() : null;
      }

      @Override
      public ListItem fromString(final String string) {
        return null;
      }
    });
    this.scriptSelector.setValue(this.scripts.get(this.preselectScript));

    for(int i = 0; i < 33; i++) {
      this.storage.add(new ListItem(paramIndex -> this.getScriptStorage(this.scriptSelector.getValue().index, paramIndex), i));
    }

    this.scriptStorage.setItems(this.storage);
    this.scriptStorage.setCellFactory(param -> {
      final TextFieldListCell<ListItem> cell = new TextFieldListCell<>();
      cell.setConverter(this.scriptSelector.getConverter());
      return cell;
    });

    for(int i = 0; i < 10; i++) {
      this.stack.add(new ListItem(stackIndex -> this.getCommandStack(this.scriptSelector.getValue().index, stackIndex), i));
    }

    this.commandStack.setItems(this.stack);
    this.commandStack.setCellFactory(this.scriptStorage.getCellFactory());

    EventManager.INSTANCE.register(this);
  }

  public void selectScript(final int index) {
    this.scriptSelector.setValue(this.scripts.get(index));
  }

  private void updateScriptName(final int scriptIndex) {
    final ListItem item = this.scripts.get(scriptIndex);
    item.update();

    if(item == this.scriptSelector.getValue()) {
      this.scriptSelector.setValue(item);
    }
  }

  private String getScriptName(final int scriptIndex) {
    return scriptStatePtrArr_800bc1c0.get(scriptIndex).getPointer() != scriptState_800bc0c0.getAddress() ? Long.toHexString(scriptStatePtrArr_800bc1c0.get(scriptIndex).getPointer()) : "not allocated";
  }

  private void updateScriptVars() {
    final ScriptState<?> state = scriptStatePtrArr_800bc1c0.get(this.scriptSelector.getValue().index).deref();

    for(int storageIndex = 0; storageIndex < 33; storageIndex++) {
      this.storage.get(storageIndex).update();
    }

    final Pointer<IntRef> top = state.commandPtr_18;
    if(top.isNull()) {
      this.stackTop.setText("null");
    } else {
      this.stackTop.setText("0x%1$08x: %2$x".formatted(top.getPointer(), top.deref().get()));
    }

    for(int stackIndex = 0; stackIndex < 10; stackIndex++) {
      this.stack.get(stackIndex).update();
    }

    this.callback04.setText("0x%1$x".formatted(state.callback_04.getPointer()));
    this.callback08.setText("0x%1$x".formatted(state.callback_08.getPointer()));
    this.callback10.setText("0x%1$x".formatted(state.callback_10.getPointer()));
    this.destructor.setText("0x%1$x".formatted(state.destructor_0c.getPointer()));
    this.filePtr.setText("0x%1$x".formatted(state.scriptPtr_14.getPointer()));
    this.parentIndex.setText("0x%1$x (%1$d)".formatted(state.storage_44.get(5).get()));
    this.childIndex.setText("0x%1$x (%1$d)".formatted(state.storage_44.get(6).get()));
  }

  private String getScriptStorage(final int scriptIndex, final int storageIndex) {
    final int val = scriptStatePtrArr_800bc1c0.get(scriptIndex).deref().storage_44.get(storageIndex).get();
    return "0x%1$x (%1$d)".formatted(val);
  }

  private String getCommandStack(final int scriptIndex, final int stackIndex) {
    return Hardware.MEMORY.waitForLock(() -> {
      final Pointer<IntRef> val = scriptStatePtrArr_800bc1c0.get(scriptIndex).deref().commandStack_1c.get(stackIndex);

      if(val.isNull()) {
        return "null";
      } else {
        return "0x%1$08x: %2$x".formatted(val.getPointer(), val.deref().get());
      }
    });
  }

  @EventListener
  public void onScriptAllocated(final ScriptAllocatedEvent event) {
    Platform.runLater(() -> this.updateScriptName(event.scriptIndex));
  }

  @EventListener
  public void onScriptDeallocated(final ScriptDeallocatedEvent event) {
    Platform.runLater(() -> this.updateScriptName(event.scriptIndex));
  }

  @EventListener
  public void onScriptTick(final ScriptTickEvent event) {
    Platform.runLater(() -> {
      if(event.scriptIndex == this.scriptSelector.getValue().index) {
        this.updateScriptVars();
      }
    });
  }

  private static class ListItem {
    private final Int2ObjectFunction<String> nameFunc;
    private final StringProperty prop = new SimpleStringProperty(this, "name");
    private final int index;

    public ListItem(final Int2ObjectFunction<String> nameFunc, final int index) {
      this.nameFunc = nameFunc;
      this.index = index;
      this.update();
    }

    public void update() {
      this.prop.set(this.index + ": " + nameFunc.get(index));
    }

    public String getName() {
      return this.prop.get();
    }
  }
}
