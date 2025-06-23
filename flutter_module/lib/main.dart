import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

void main() {
  runApp(const ProviderScope(child: MyApp()));
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(home: UserListScreen());
  }
}

class User {
  final int id;
  final String name;
  final String address;
  final String phone;
  final String signature;
  final String profilePic;

  User({
    required this.id,
    required this.name,
    required this.address,
    required this.phone,
    required this.signature,
    required this.profilePic,
  });

  factory User.fromMap(Map<dynamic, dynamic> map) {
    return User(
      id: map['id'],
      name: map['name'],
      address: map['address'],
      phone: map['phone'],
      signature: map['signature'],
      profilePic: map['profilePic'],
    );
  }
}

class UserNotifier extends StateNotifier<List<User>> {
  UserNotifier() : super([]) {
    _init();
  }

  static const platform = MethodChannel('user_channel');

  Future<void> _init() async {
    // Delay to ensure native side is ready
    await Future.delayed(const Duration(milliseconds: 5000));
    await fetchUsers();
  }

  Future<void> fetchUsers() async {
    try {
      final List<dynamic> result = await platform.invokeMethod('getUsers');
      state = result.map((e) => User.fromMap(e)).toList();
    } catch (e) {
      print('Error fetching users: $e');
    }
  }

  Future<void> deleteUser(int id) async {
    try {
      final bool success = await platform.invokeMethod('deleteUser', {'id': id});
      if (success) {
        await fetchUsers();
      }
    } catch (e) {
      print('Error deleting user: $e');
    }
  }
}

final userProvider = StateNotifierProvider<UserNotifier, List<User>>((ref) {
  return UserNotifier();
});

class UserListScreen extends ConsumerStatefulWidget {
  const UserListScreen({super.key});

  @override
  ConsumerState<UserListScreen> createState() => _UserListScreenState();
}

class _UserListScreenState extends ConsumerState<UserListScreen>
    with WidgetsBindingObserver {

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _fetchUsers();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _fetchUsers();
    }
  }

  void _fetchUsers() {
    ref.read(userProvider.notifier).fetchUsers();
  }

  @override
  Widget build(BuildContext context) {
    final users = ref.watch(userProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('User List')),
      body: users.isEmpty
          ? const Center(child: Text("No users found"))
          : ListView.builder(
        itemCount: users.length,
        itemBuilder: (context, index) {
          final user = users[index];
          return Card(
            child: ListTile(
              leading: const Icon(Icons.person),
              title: Text(user.name),
              subtitle: Text('${user.address} | ${user.phone}'),
              trailing: IconButton(
                icon: const Icon(Icons.delete, color: Colors.red),
                onPressed: () =>
                    ref.read(userProvider.notifier).deleteUser(user.id),
              ),
            ),
          );
        },
      ),
    );
  }
}
