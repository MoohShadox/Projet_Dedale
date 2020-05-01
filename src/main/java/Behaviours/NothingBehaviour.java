package Behaviours;

import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;

public class NothingBehaviour extends OneShotBehaviour {
    @Override
    public void action() {

    }

    @Override
    public int onEnd() {
        return 3;
    }
}
