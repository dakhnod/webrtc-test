package d.d.webrtcsient;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    PeerConnectionFactory factory;
    PeerConnection localPeer, remotePeer;
    DataChannel localChannel, remoteChannel;

    MediaConstraints constraints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initRTC();
    }

    private void initRTC() {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions());
        factory = PeerConnectionFactory.builder().createPeerConnectionFactory();
        constraints = new MediaConstraints();
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
    }

    public void startServer(View view) {
        new Thread(() -> {
            try {
                ServerSocket server = new ServerSocket(9999);
                runOnUiThread(() -> {
                    ((Button) view).setText("started");
                    view.setClickable(false);
                });
                server.setSoTimeout(60000);
                Socket s = server.accept();
                InputStream input = s.getInputStream();
                while (input.available() == 0) {
                    Thread.sleep(100);
                }
                byte[] bytes = new byte[input.available()];
                input.read(bytes);
                String remoteDesc = new String(bytes);
                log("remote desc: " + remoteDesc);

                log("creating remote peer");
                try {
                    List<PeerConnection.IceServer> servers = Collections.emptyList();
                    remotePeer = factory.createPeerConnection(servers, new PeerConnection.Observer() {

                        @Override
                        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

                        }

                        @Override
                        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

                        }

                        @Override
                        public void onIceConnectionReceivingChange(boolean b) {

                        }

                        @Override
                        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                            if (iceGatheringState != PeerConnection.IceGatheringState.COMPLETE)
                                return;

                            log("answer description: " + remotePeer.getLocalDescription().description);

                            try {
                                OutputStream os = s.getOutputStream();
                                os.write(remotePeer.getLocalDescription().description.getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onIceCandidate(IceCandidate iceCandidate) {

                        }

                        @Override
                        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

                        }

                        @Override
                        public void onAddStream(MediaStream mediaStream) {

                        }

                        @Override
                        public void onRemoveStream(MediaStream mediaStream) {

                        }

                        @Override
                        public void onDataChannel(DataChannel dataChannel) {
                            remoteChannel = dataChannel;
                            log("remote Channel set.");
                            remoteChannel.registerObserver(new DataChannel.Observer() {
                                @Override
                                public void onBufferedAmountChange(long l) {

                                }

                                @Override
                                public void onStateChange() {

                                }

                                @Override
                                public void onMessage(DataChannel.Buffer buffer) {
                                    log("onMessage");
                                    byte[] data = new byte[buffer.data.capacity()];
                                    buffer.data.get(data);
                                    runOnUiThread(() -> {
                                        Toast.makeText(MainActivity.this, new String(data), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                remoteChannel.send(new DataChannel.Buffer(ByteBuffer.wrap("test".getBytes()), false));
                            }).start();
                        }

                        @Override
                        public void onRenegotiationNeeded() {

                        }

                        @Override
                        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

                        }
                    });
                } catch (Exception e) {
                    log("error: ");
                }

                log("setting remote local desc");
                remotePeer.setRemoteDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {

                    }

                    @Override
                    public void onSetSuccess() {
                        log("onSetSuccess");
                        remotePeer.createAnswer(new SdpObserver() {
                            @Override
                            public void onCreateSuccess(SessionDescription sessionDescription) {
                                remotePeer.setLocalDescription(new SdpObserver() {
                                    @Override
                                    public void onCreateSuccess(SessionDescription sessionDescription) {

                                    }

                                    @Override
                                    public void onSetSuccess() {
                                        log("remote local desc set");
                                    }

                                    @Override
                                    public void onCreateFailure(String s) {

                                    }

                                    @Override
                                    public void onSetFailure(String s) {
                                        log("remote local desc failure: " + s);
                                    }
                                }, sessionDescription);
                            }

                            @Override
                            public void onSetSuccess() {

                            }

                            @Override
                            public void onCreateFailure(String s) {

                            }

                            @Override
                            public void onSetFailure(String s) {

                            }
                        }, constraints);
                    }

                    @Override
                    public void onCreateFailure(String s) {

                    }

                    @Override
                    public void onSetFailure(String s) {

                    }
                }, new SessionDescription(SessionDescription.Type.OFFER, remoteDesc));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    ((Button) view).setText("stopped due to exception");
                    view.setClickable(true);
                });
            }
        }).start();
    }

    public void connect(View view) {
        log("connecting...");
        new Thread(() -> {
            List<PeerConnection.IceServer> iceServers = Collections.emptyList();
            localPeer = factory.createPeerConnection(iceServers, new PeerConnection.Observer() {
                @Override
                public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                }

                @Override
                public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

                }

                @Override
                public void onIceConnectionReceivingChange(boolean b) {

                }

                @Override
                public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                    if (iceGatheringState != PeerConnection.IceGatheringState.COMPLETE) return;
                    log("onIceGatheringChange: " + localPeer.getLocalDescription().description);

                    try {
                        Socket s = new Socket("192.168.0.103", 9999);
                        OutputStream o = s.getOutputStream();
                        log("sending description...");
                        o.write(localPeer.getLocalDescription().description.getBytes());
                        log("waiting for response...");
                        InputStream is = s.getInputStream();
                        while (is.available() == 0) {
                            Thread.sleep(100);
                        }
                        byte[] bytes = new byte[is.available()];
                        is.read(bytes);
                        String remoteDesc = new String(bytes);
                        log("got response");
                        localPeer.setRemoteDescription(new SdpObserver() {
                            @Override
                            public void onCreateSuccess(SessionDescription sessionDescription) {

                            }

                            @Override
                            public void onSetSuccess() {
                                log("succ setting local remote desc");
                            }

                            @Override
                            public void onCreateFailure(String s) {

                            }

                            @Override
                            public void onSetFailure(String s) {

                            }
                        }, new SessionDescription(SessionDescription.Type.ANSWER, remoteDesc));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onIceCandidate(IceCandidate iceCandidate) {

                }

                @Override
                public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

                }

                @Override
                public void onAddStream(MediaStream mediaStream) {

                }

                @Override
                public void onRemoveStream(MediaStream mediaStream) {

                }

                @Override
                public void onDataChannel(DataChannel dataChannel) {
                    localChannel = dataChannel;
                    log("local Channel set.");
                }

                @Override
                public void onRenegotiationNeeded() {

                }

                @Override
                public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

                }
            });
            localChannel = localPeer.createDataChannel("test", new DataChannel.Init());
            localChannel.registerObserver(new DataChannel.Observer() {
                @Override
                public void onBufferedAmountChange(long l) {

                }

                @Override
                public void onStateChange() {

                }

                @Override
                public void onMessage(DataChannel.Buffer buffer) {
                    log("onMessage");
                    byte[] data = new byte[buffer.data.capacity()];
                    buffer.data.get(data);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, new String(data), Toast.LENGTH_SHORT).show();
                    });
                    localChannel.send(new DataChannel.Buffer(ByteBuffer.wrap("self test".getBytes()), false));
                }
            });
            localPeer.createOffer(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    log("onCreateSuccess: " + sessionDescription.description);
                    localPeer.setLocalDescription(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {
                            log("succ creating local description");
                        }

                        @Override
                        public void onSetSuccess() {
                            log("succ setting local description");

                        }

                        @Override
                        public void onCreateFailure(String s) {
                            log("fail creating local description " + s);
                        }

                        @Override
                        public void onSetFailure(String s) {
                            log("fail setting local description " + s);
                        }
                    }, sessionDescription);
                }

                @Override
                public void onSetSuccess() {

                }

                @Override
                public void onCreateFailure(String s) {
                    log("failure crating offer: " + s);
                }

                @Override
                public void onSetFailure(String s) {

                }
            }, constraints);

        }).start();
    }

    private void log(String data) {
        Log.d("MainActivity", data);
    }
}
